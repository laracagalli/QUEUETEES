package gui.staff;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import javax.swing.*;
import model.*;
import service.StoreService;

public class CompletedOrderFilterTest {
    private static Object field(Object panel, String name) throws Exception {
        Field field = panel.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(panel);
    }
    private static JTextField search(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JTextField && Boolean.TRUE.equals(((JTextField) child).getClientProperty("searchField")))
                return (JTextField) child;
            if (child instanceof Container) {
                JTextField found = search((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void complete(int customer, String name, String payment, String fulfillment) {
        StoreService store = StoreService.getInstance();
        store.addToCart(customer, store.getProducts().get(0).getId());
        Order order = store.checkout(customer, new CheckoutDetails(name, "test@example.com", "+639123456789",
                fulfillment, "123 Test Street", payment, ""));
        store.advanceOrder(order.getId());
        store.advanceOrder(order.getId());
        store.advanceOrder(order.getId());
        store.completeOrder(order.getId());
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container) layout((Container) child);
    }
    public static void main(String[] args) throws Exception {
        complete(700, "Maria Cruz", "GCash ref. 123456", "Delivery");
        complete(701, "Maria Cruz", "Card", "Pickup");
        complete(702, "Juan Reyes", "GCash", "Pickup");
        SwingUtilities.invokeAndWait(() -> {
            try {
                gui.components.AppTheme.install();
                User staff = new User(90, "staff", "staff@example.com", "unused", UserRole.STAFF, true, AccountStatus.ACTIVE);
                CompletedOrdersPanel panel = new CompletedOrdersPanel(staff);
                JTable table = (JTable) field(panel, "table");
                JComboBox<?> customer = (JComboBox<?>) field(panel, "customerFilter");
                JComboBox<?> payment = (JComboBox<?>) field(panel, "paymentFilter");
                JComboBox<?> fulfillment = (JComboBox<?>) field(panel, "fulfillmentFilter");
                check(table.getRowCount() == 3, "All completed orders shown initially");
                customer.setSelectedItem("Maria Cruz");
                check(table.getRowCount() == 2, "Customer filter");
                payment.setSelectedItem("GCash");
                fulfillment.setSelectedItem("Delivery");
                check(table.getRowCount() == 1, "Combined filters normalize GCash reference payments");
                search(panel).setText("Juan");
                check(table.getRowCount() == 0 && !((JButton) field(panel, "print")).isEnabled(), "Search combines with dropdown filters");
                search(panel).setText("");
                table.setRowSelectionInterval(0, 0);
                panel.refresh();
                check(table.getRowCount() == 1 && table.getSelectedRow() == 0, "Refresh preserves filters and selected order");
                fulfillment.setSelectedItem("Pickup");
                check(table.getRowCount() == 0, "Incompatible filters give no results");
                customer.setSelectedIndex(0);
                payment.setSelectedIndex(0);
                fulfillment.setSelectedIndex(0);
                JCheckBox allDates = (JCheckBox) field(panel, "allDates");
                allDates.doClick();
                java.util.Date yesterday = java.util.Date.from(java.time.LocalDate.now().minusDays(1)
                        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                ((JSpinner) field(panel, "from")).setValue(yesterday);
                ((JSpinner) field(panel, "to")).setValue(yesterday);
                check(table.getRowCount() == 0, "Completion-date filter still applies");
                allDates.doClick();
                check(table.getRowCount() == 3, "All dates restores rows");
                gui.components.AppTheme.apply(panel);
                panel.setSize(1120, 700);
                layout(panel);
                BufferedImage image = new BufferedImage(1120, 700, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(new Color(230, 234, 224)); graphics.fillRect(0, 0, 1120, 700);
                panel.printAll(graphics); graphics.dispose();
                javax.imageio.ImageIO.write(image, "png", new java.io.File("bin/review/completed-filters.png"));
                System.out.println("PASS: completed-order filters, search, dates, selection and report availability");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
