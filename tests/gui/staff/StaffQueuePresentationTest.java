package gui.staff;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import javax.swing.*;
import model.*;
import service.StoreService;

public final class StaffQueuePresentationTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                render(new StaffOverviewPanel(() -> {}), "overview-empty.png");
                render(new OrderQueuePanel(), "order-queue-empty.png");
                StoreService store = StoreService.getInstance();
                for (int i = 0; i < 3; i++) {
                    store.addToCart(800 + i, store.getProducts().get(0).getId());
                    if (i == 1) store.addToCart(800 + i, store.getProducts().get(1).getId());
                    Order order = store.checkout(800 + i, new CheckoutDetails("Customer " + i, "test@example.com",
                            "+639123456789", "Pickup", "", "GCash", ""));
                    if (i == 0) store.advanceOrder(order.getId());
                }
                OrderQueuePanel queue = new OrderQueuePanel();
                JTable table = field(queue, "table", JTable.class);
                check(table.getValueAt(0, 0) == null, "Preparing order has no waiting position");
                check(table.getValueAt(1, 0).equals(1) && table.getValueAt(2, 0).equals(2), "Waiting orders numbered in FIFO order");
                JTextField search = search(queue);
                search.setText("Customer 2");
                check(table.getRowCount() == 1 && table.getValueAt(0, 0).equals(2), "Filtering does not renumber the queue");
                table.setRowSelectionInterval(0, 0);
                check(!field(queue, "advance", JButton.class).isEnabled(), "Later waiting order cannot start");
                field(queue, "selectNext", JButton.class).doClick();
                check(table.getValueAt(table.getSelectedRow(), 0).equals(1), "Shortcut clears search and selects true first waiting order");
                check(field(queue, "advance", JButton.class).isEnabled(), "First waiting order can start");
                table.getRowSorter().toggleSortOrder(2);
                table.getRowSorter().toggleSortOrder(2);
                for (int row = 0; row < table.getRowCount(); row++) {
                    int modelRow = table.convertRowIndexToModel(row);
                    check(java.util.Objects.equals(table.getValueAt(row, 0), table.getModel().getValueAt(modelRow, 0)), "Sorted positions match orders");
                    int statusView = table.convertColumnIndexToView(6);
                    Component cell = table.prepareRenderer(table.getCellRenderer(row, statusView), row, statusView);
                    check(cell.getForeground().equals(StaffQueuePresentation.statusColor(String.valueOf(table.getValueAt(row, statusView)))), "Status color survives selection and sort");
                }
                OrderDetailsPanel details = new OrderDetailsPanel();
                JTable detailTable = field(details, "table", JTable.class);
                check(detailTable.getValueAt(1, 0).equals(1) && detailTable.getValueAt(2, 0).equals(1), "Multiple items share their order's position");
                detailTable.setRowSelectionInterval(3, 3);
                check(!field(details, "advance", JButton.class).isEnabled(), "Details pane blocks later waiting orders");
                detailTable.setRowSelectionInterval(1, 1);
                check(field(details, "advance", JButton.class).isEnabled(), "Details pane allows first waiting order");
                details.refresh();
                check(detailTable.getSelectedRow() == 1, "Details refresh preserves selection");
                render(queue, "order-queue-screen.png");
                render(details, "order-details-screen.png");
                render(new StaffOverviewPanel(() -> {}), "overview-orders.png");
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("PASS: queue ranking, filtered/sorted priority, next-order shortcut, status colors and repeated item positions");
    }
    private static JTextField search(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTextField) return (JTextField) child;
            if (child instanceof Container) { JTextField result = search((Container) child); if (result != null) return result; }
        }
        return null;
    }
    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return type.cast(f.get(owner));
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container) layout((Container) child); }
    private static void render(JPanel panel, String filename) throws Exception {
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setBackground(new Color(241, 241, 232));
        wrap.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25)); wrap.add(panel); wrap.setSize(1120, 720); layout(wrap);
        BufferedImage image = new BufferedImage(1120, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        wrap.printAll(g); g.dispose(); new java.io.File("bin/review").mkdirs();
        ImageIO.write(image, "png", new java.io.File("bin/review/" + filename));
    }
}
