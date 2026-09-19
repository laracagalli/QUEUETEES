package gui.staff;

import java.awt.*;
import java.awt.print.PrinterJob;
import javax.swing.*;

/** Preview a stable report snapshot before opening the system print dialog. */
final class ReportPreview {
    static JPanel create(Component parent, CompletedOrdersReport report, Runnable closePreview) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setName("completedOrdersReportPreview");
        JLabel page = new JLabel();
        page.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel counter = new JLabel();
        JButton previous = StaffStyles.lightButton("Previous");
        JButton next = StaffStyles.lightButton("Next");
        JButton print = StaffStyles.button("Print report");
        JButton back = StaffStyles.lightButton("Back to orders");
        back.addActionListener(e -> closePreview.run());
        int[] index = {0};
        Runnable update = () -> {
            page.setIcon(new ImageIcon(report.preview(index[0]).getScaledInstance(
                    CompletedOrdersReport.WIDTH, CompletedOrdersReport.HEIGHT, Image.SCALE_SMOOTH)));
            counter.setText("Page " + (index[0] + 1) + " of " + report.getPageCount());
            previous.setEnabled(index[0] > 0);
            next.setEnabled(index[0] + 1 < report.getPageCount());
        };
        previous.addActionListener(e -> { index[0]--; update.run(); });
        next.addActionListener(e -> { index[0]++; update.run(); });
        print.addActionListener(e -> {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                if (job.getPrintService() == null) {
                    StaffStyles.showMessage(parent, "No printer is available. Set up a printer or enable Microsoft Print to PDF, then try again.",
                            "Print report", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                job.setJobName("QueueTees completed orders report");
                job.setPrintable(report);
                javax.print.attribute.HashPrintRequestAttributeSet attributes = new javax.print.attribute.HashPrintRequestAttributeSet();
                Window owner = SwingUtilities.getWindowAncestor(parent);
                if (owner != null) attributes.add(new javax.print.attribute.standard.DialogOwner(owner));
                if (!job.printDialog(attributes)) return;
                print.setEnabled(false);
                back.setEnabled(false);
                counter.setText("Sending report to printer...");
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception { job.print(attributes); return null; }
                    @Override protected void done() {
                        print.setEnabled(true);
                        back.setEnabled(true);
                        update.run();
                        try {
                            get();
                            StaffStyles.showMessage(parent, "Report sent to the selected printer.");
                        } catch (Exception ex) {
                            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                            StaffStyles.showMessage(parent, "Report was not printed: " + cause.getMessage(),
                                    "Print report", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }.execute();
            } catch (RuntimeException ex) {
                StaffStyles.showMessage(parent, "Cannot open printing: " + ex.getMessage(),
                        "Print report", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        actions.add(previous); actions.add(counter); actions.add(next); actions.add(print);
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        heading.add(StaffStyles.label("Completed orders report", 22, true, StaffStyles.FOREST));
        heading.add(back, BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);
        JScrollPane scroll = new gui.components.ModernScrollPane(page);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        update.run();
        return panel;
    }
    private ReportPreview() { }
}
