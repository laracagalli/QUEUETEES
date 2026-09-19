package gui.admin;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.User;

/** Immutable visible-table snapshot used for both preview and printing. */
final class AdminTableReport implements Printable {
    private final JTable snapshot;
    private final String title, by, date=java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm z"));
    private final Image logo;
    final int rows;
    AdminTableReport(JTable source,String title,User user) {
        this.title=title;by=user==null?"Administrator (identity unavailable)":user.getUsername()+" | "+user.getEmail();rows=source.getRowCount();
        String[] columns=new String[source.getColumnCount()];Object[][] data=new Object[rows][columns.length];
        for(int c=0;c<columns.length;c++){columns[c]=source.getColumnName(c);for(int r=0;r<rows;r++)data[r][c]=source.getValueAt(r,c);}
        snapshot=new JTable(new DefaultTableModel(data,columns));AdminUi.style(snapshot);snapshot.setRowHeight(44);snapshot.setSize(Math.max(900,source.getWidth()),rows*44);snapshot.doLayout();
        snapshot.getTableHeader().setSize(snapshot.getWidth(),42);
        java.net.URL url=getClass().getResource("/Gui_Images/hirayalogo2.png");logo=url==null?null:new ImageIcon(url).getImage();
    }
    static PageFormat format(){Paper paper=new Paper();paper.setSize(842,595);paper.setImageableArea(30,30,782,535);PageFormat f=new PageFormat();f.setPaper(paper);return f;}
    public int print(Graphics graphics,PageFormat format,int page) throws PrinterException {
        PageFormat bodyFormat=(PageFormat)format.clone();Paper p=bodyFormat.getPaper();p.setImageableArea(format.getImageableX(),format.getImageableY()+110,format.getImageableWidth(),format.getImageableHeight()-110);bodyFormat.setPaper(p);
        Graphics2D g=(Graphics2D)graphics.create();int result;
        if(rows==0){if(page>0){g.dispose();return NO_SUCH_PAGE;}result=PAGE_EXISTS;g.setColor(Color.DARK_GRAY);g.drawString("No records in this report.",(int)format.getImageableX(),(int)format.getImageableY()+135);}else result=snapshot.getPrintable(JTable.PrintMode.FIT_WIDTH,null,new java.text.MessageFormat("Page {0}")).print(g,bodyFormat,page);
        if(result==PAGE_EXISTS){int x=(int)format.getImageableX(),y=(int)format.getImageableY(),w=(int)format.getImageableWidth();g.setColor(Color.BLACK);if(logo!=null)g.drawImage(logo,x+w/2-60,y,120,42,null);g.setFont(new Font("Fira Code",Font.BOLD,13));g.drawString(title,x,y+60);g.setFont(new Font("Fira Code",Font.PLAIN,9));g.drawString("Printed: "+date+"   |   Prepared by: "+by,x,y+77);g.drawString("Visible table snapshot | "+rows+" records",x,y+92);}
        g.dispose();return result;
    }
    BufferedImage page(int index)throws PrinterException {BufferedImage image=new BufferedImage(842,595,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,842,595);int result=print(g,format(),index);g.dispose();return result==PAGE_EXISTS?image:null;}
    static void open(JPanel host,JTable table,String title,User user) {
        AdminTableReport report=new AdminTableReport(table,title,user);
        Component[] originals=host.getComponents();LayoutManager layout=host.getLayout();host.removeAll();host.setLayout(new BorderLayout(0,12));
        JPanel preview=new JPanel(new BorderLayout(0,12));preview.setOpaque(false);preview.setName("adminReportPreview");JLabel image=new JLabel();image.setHorizontalAlignment(SwingConstants.CENTER);JLabel count=AdminUi.label("",11,false);
        JButton back=AdminUi.button("Back to table"),previous=AdminUi.button("Previous"),next=AdminUi.button("Next"),print=AdminUi.button("Print report");int[] index={0};
        Runnable update=()->{try{image.setIcon(new ImageIcon(report.page(index[0])));previous.setEnabled(index[0]>0);next.setEnabled(report.page(index[0]+1)!=null);count.setText("Page "+(index[0]+1));}catch(PrinterException e){JOptionPane.showMessageDialog(host,"Cannot preview report: "+e.getMessage());}};
        previous.addActionListener(e->{index[0]--;update.run();});next.addActionListener(e->{index[0]++;update.run();});back.addActionListener(e->{host.removeAll();host.setLayout(layout);for(Component c:originals)host.add(c);host.revalidate();host.repaint();});
        print.addActionListener(e->{PrinterJob job=PrinterJob.getPrinterJob();job.setJobName(title);job.setPrintable(report,format());if(job.getPrintService()==null){JOptionPane.showMessageDialog(host,"Set up a printer or Microsoft Print to PDF first.");return;}if(!job.printDialog())return;print.setEnabled(false);back.setEnabled(false);new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{job.print();return null;}protected void done(){print.setEnabled(true);back.setEnabled(true);try{get();}catch(Exception ex){JOptionPane.showMessageDialog(host,"Printing failed: "+ex.getMessage());}}}.execute();});
        JPanel heading=new JPanel(new BorderLayout());heading.setOpaque(false);heading.add(AdminUi.label(title,25,true));heading.add(back,BorderLayout.EAST);preview.add(heading,BorderLayout.NORTH);preview.add(new gui.components.ModernScrollPane(image));JPanel actions=new JPanel(new FlowLayout());actions.add(previous);actions.add(count);actions.add(next);actions.add(print);preview.add(actions,BorderLayout.SOUTH);host.add(preview);update.run();host.revalidate();host.repaint();
    }
}
