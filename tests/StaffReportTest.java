import gui.staff.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import model.*;
import service.StoreService;

/** Report pagination and staff presentation regression tests; no physical printing. */
public final class StaffReportTest {
    public static void main(String[] args) throws Exception {
        User staff = new User(42, "lara.staff", "lara@example.com", "unused", UserRole.STAFF, true, AccountStatus.ACTIVE);
        Product product = new Product(77, "Hiraya shirt", "Men", "Tops", 549, 100, "");
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 43; i++) {
            String customer = i == 0 ? "Alexandra Maria Dela Cruz - Long Customer Name For Wrapping" : "Customer " + (i + 1);
            Order order = new Order(i + 1, i + 1, 5, customer,
                    List.of(new CartItem(product, 2)), LocalDateTime.now().minusHours(2),
                    new CheckoutDetails(customer, "customer@example.com", "09123456789", "Delivery", "Test address", "QR Code", ""));
            order.setStatus(OrderStatus.COMPLETED);
            orders.add(order);
        }
        CompletedOrdersReport report = new CompletedOrdersReport(orders, staff, "All completion dates; search: Customer");
        check(report.getPageCount() > 1, "Multiple pages");
        check(report.getOrderCount() == 43, "All completed orders included");
        check(report.getTotal() == 43 * 1098, "Report total");
        BufferedImage before = report.preview(0);
        product.setPrice(1);
        check(orders.get(0).getTotal() == 1098, "Historical order prices survive catalog edits");
        orders.clear();
        BufferedImage after = report.preview(0);
        check(java.util.Arrays.equals(before.getRGB(0, 0, before.getWidth(), before.getHeight(), null, 0, before.getWidth()),
                after.getRGB(0, 0, after.getWidth(), after.getHeight(), null, 0, after.getWidth())), "Preview snapshot remains stable");
        java.io.File output = new java.io.File("bin/review"); output.mkdirs();
        for (int page = 0; page < report.getPageCount(); page++)
            ImageIO.write(report.preview(page), "png", new java.io.File(output, "report-page-" + (page + 1) + ".png"));
        PageFormat format = new PageFormat();
        Paper paper = new Paper(); paper.setSize(595, 842); paper.setImageableArea(40, 40, 515, 762); format.setPaper(paper);
        BufferedImage printed = new BufferedImage(595, 842, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = printed.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 595, 842);
        check(report.print(graphics, format, 0) == Printable.PAGE_EXISTS, "First printable page");
        check(report.print(graphics, format, report.getPageCount()) == Printable.NO_SUCH_PAGE, "Pagination terminates");
        graphics.dispose();
        ImageIO.write(printed, "png", new java.io.File(output, "report-paper.png"));
        try { new CompletedOrdersReport(List.of(), staff, "All"); throw new AssertionError("Empty report accepted"); }
        catch (IllegalArgumentException expected) { }
        StoreService store = StoreService.getInstance();
        int productId = store.getProducts().get(0).getId();
        for (int i = 0; i < 4; i++) {
            store.addToCart(500 + i, productId);
            Order o = store.checkout(500 + i, new CheckoutDetails("Customer " + (i + 1), "test@example.com", "09123456789", "Pickup", "", "Cash", ""));
            if (i < 3) store.advanceOrder(o.getId());
            if (i < 2) store.advanceOrder(o.getId());
            if (i == 0) store.advanceOrder(o.getId());
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                CompletedOrdersPanel completed = new CompletedOrdersPanel(staff);
                JTable table = field(completed, "table", JTable.class);
                JButton print = field(completed, "print", JButton.class);
                check(table.getRowCount() == 1 && print.isEnabled(), "Print enabled for completed results");
                JCheckBox all = field(completed, "allDates", JCheckBox.class);
                JSpinner from = field(completed, "from", JSpinner.class);
                JSpinner to = field(completed, "to", JSpinner.class);
                all.doClick();
                check(table.getRowCount() == 1, "Inclusive current completion date");
                to.setValue(java.util.Date.from(java.time.Instant.now().minusSeconds(86400)));
                check(table.getRowCount() == 0 && !print.isEnabled(), "Invalid range blocks printing");
                all.doClick();
                check(table.getRowCount() == 1 && print.isEnabled(), "All dates restores report");
                to.setValue(from.getValue());
                render(completed, "completed-screen.png");
                print.doClick();
                JPanel preview = field(completed, "preview", JPanel.class);
                check(preview != null && preview.getParent() == completed, "Report click opens visible in-panel preview");
                check(findButton(preview, "Print report") != null, "Preview exposes printer action");
                check(!findButton(preview, "Next").isEnabled(), "Single-page report cannot advance");
                completed.refresh();
                check(field(completed, "preview", JPanel.class) == preview, "Auto-refresh leaves preview open");
                render(completed, "report-preview-screen.png");
                findButton(preview, "Back to orders").doClick();
                check(field(completed, "preview", JPanel.class) == null && table.getRowCount() == 1,
                        "Back restores completed orders");
                print.doClick();
                check(field(completed, "preview", JPanel.class) != null, "Report can reopen after returning");
                findButton(field(completed, "preview", JPanel.class), "Back to orders").doClick();
                render(new QueueStatusPanel(), "queue-screen.png");
                StaffProfilePanel profile = new StaffProfilePanel(staff);
                checkReadOnly(profile);
                render(profile, "profile-screen.png");
                StaffOverviewPanel overview = new StaffOverviewPanel(() -> {});
                checkReadOnly(overview);
                render(overview, "overview-screen.png");
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("PASS: report button click, preview/back/reopen, pagination, immutable snapshot, totals, print rendering, date filters and read-only controls");
    }
    private static JButton findButton(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton) child).getText())) return (JButton) child;
            if (child instanceof Container) {
                JButton found = findButton((Container) child, text);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void render(JPanel panel, String name) throws Exception {
        JPanel background = new JPanel(new BorderLayout());
        background.setBackground(new Color(241, 241, 232));
        background.setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));
        background.add(panel);
        background.setSize(1120, 720);
        layout(background);
        BufferedImage image = new BufferedImage(1120, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        background.printAll(g); g.dispose();
        ImageIO.write(image, "png", new java.io.File("bin/review/" + name));
    }
    private static void layout(Container parent) { parent.doLayout(); for (Component c : parent.getComponents()) if (c instanceof Container) layout((Container) c); }
    private static void checkReadOnly(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof javax.swing.text.JTextComponent) {
                javax.swing.text.JTextComponent text = (javax.swing.text.JTextComponent) c;
                check(!text.isEditable() && !text.isFocusable() && text.getCursor().getType() == Cursor.DEFAULT_CURSOR, "Display text cannot take typing focus");
            }
            if (c instanceof Container) checkReadOnly((Container) c);
        }
    }
    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return type.cast(field.get(owner));
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
