package gui.staff;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import model.Order;
import model.OrderStatus;
import model.User;

/** Immutable report snapshot. Preview and printer use the same paginated renderer. */
public final class CompletedOrdersReport implements Printable {
    public static final int WIDTH = 515, HEIGHT = 742;
    private static final int[] COLUMN_WIDTHS = {48, 127, 90, 105, 35, 110};
    private static final Font BODY = new Font("SansSerif", Font.PLAIN, 9);
    private final List<List<Row>> pages = new ArrayList<>();
    private final List<String> metadata = new ArrayList<>();
    private final String reportId;
    private final int orderCount, itemCount, tableTop;
    private final double total;
    private final BufferedImage logo;

    public CompletedOrdersReport(List<Order> orders, User staff, String scope) {
        if (staff == null) throw new IllegalArgumentException("A signed-in staff account is required to print.");
        if (orders.isEmpty()) throw new IllegalArgumentException("There are no completed orders in this report.");
        ZonedDateTime printed = ZonedDateTime.now();
        reportId = "QT-" + printed.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = measure.createGraphics();
        g.setFont(BODY);
        FontMetrics fm = g.getFontMetrics();
        metadata.addAll(wrap("Print date: " + printed.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss z (XXX)")), fm, WIDTH));
        metadata.addAll(wrap("Printed by: " + staff.getUsername() + " | Staff ID: " + staff.getId() + " | " + staff.getEmail(), fm, WIDTH));
        metadata.addAll(wrap("Scope: " + scope, fm, WIDTH));
        tableTop = 122 + metadata.size() * 12 + 42;
        if (tableTop > 400) throw new IllegalArgumentException("Report description is too long. Shorten the search filter.");
        List<Row> page = new ArrayList<>();
        int used = tableTop + 24;
        int items = 0;
        double sum = 0;
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.COMPLETED || order.getCompletedAt() == null)
                throw new IllegalArgumentException("Reports can contain completed orders only.");
            String[] cells = {String.format("Q-%03d", order.getQueueNumber()), order.getCustomerName(),
                    order.getCompletedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy\nHH:mm")),
                    order.getCheckoutDetails().getFulfillmentMethod() + "\n" + order.getCheckoutDetails().getPaymentMethod(),
                    String.valueOf(order.getItemCount()), String.format(java.util.Locale.US, "%,.2f", order.getTotal())};
            Row row = new Row(cells, fm);
            if (row.height > 682 - tableTop - 24) throw new IllegalArgumentException("An order has too much text for a report page.");
            if (used + row.height > 682) {
                pages.add(page);
                page = new ArrayList<>();
                used = tableTop + 24;
            }
            page.add(row);
            used += row.height;
            items += order.getItemCount();
            sum += order.getTotal();
        }
        pages.add(page);
        orderCount = orders.size();
        itemCount = items;
        total = sum;
        g.dispose();
        try {
            java.net.URL resource = CompletedOrdersReport.class.getResource("/Gui_Images/hirayalogo2.png");
            if (resource == null) throw new java.io.IOException("Logo resource missing");
            logo = ImageIO.read(resource);
            if (logo == null) throw new java.io.IOException("Logo cannot be decoded");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("The Hiraya logo could not be loaded. Include src on the runtime classpath.", e);
        }
    }

    public int getPageCount() { return pages.size(); }
    public int getOrderCount() { return orderCount; }
    public double getTotal() { return total; }

    public BufferedImage preview(int pageIndex) {
        BufferedImage image = new BufferedImage(WIDTH * 2, HEIGHT * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(2, 2);
        render(g, pageIndex);
        g.dispose();
        return image;
    }

    @Override public int print(Graphics graphics, PageFormat format, int index) throws PrinterException {
        if (index < 0 || index >= pages.size()) return NO_SUCH_PAGE;
        if (format.getImageableWidth() <= 0 || format.getImageableHeight() <= 0)
            throw new PrinterException("The selected paper has no printable area.");
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min(format.getImageableWidth() / WIDTH, format.getImageableHeight() / HEIGHT);
        g.translate(format.getImageableX() + (format.getImageableWidth() - WIDTH * scale) / 2,
                format.getImageableY());
        g.scale(scale, scale);
        render(g, index);
        g.dispose();
        return PAGE_EXISTS;
    }

    private void render(Graphics2D g, int index) {
        if (index < 0 || index >= pages.size()) throw new IndexOutOfBoundsException("Report page");
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        double ratio = Math.min(150.0 / logo.getWidth(), 63.0 / logo.getHeight());
        int w = (int) (logo.getWidth() * ratio), h = (int) (logo.getHeight() * ratio);
        g.drawImage(logo, (WIDTH - w) / 2, 0, w, h, null);
        g.setColor(StaffStyles.FOREST);
        centered(g, "COMPLETED ORDERS REPORT", 85, new Font("SansSerif", Font.BOLD, 15));
        centered(g, "QueueTees | Hiraya Clothing", 102, BODY);
        g.setColor(Color.DARK_GRAY);
        g.setFont(BODY);
        int y = 122;
        for (String line : metadata) { g.drawString(line, 0, y); y += 12; }
        g.setColor(new Color(237, 241, 234));
        g.fillRoundRect(0, y + 4, WIDTH, 28, 6, 6);
        g.setColor(StaffStyles.FOREST);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString(String.format(java.util.Locale.US, "%d completed orders     |     %d items     |     Total order value: PHP %,.2f",
                orderCount, itemCount, total), 10, y + 22);
        g.fillRect(0, tableTop, WIDTH, 24);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 8));
        String[] headers = {"Queue", "Customer", "Completed", "Method / payment", "Items", "Amount (PHP)"};
        int x = 0;
        for (int i = 0; i < headers.length; i++) { g.drawString(headers[i], x + 5, tableTop + 15); x += COLUMN_WIDTHS[i]; }
        y = tableTop + 24;
        int rowIndex = 0;
        for (Row row : pages.get(index)) {
            g.setColor(rowIndex++ % 2 == 0 ? Color.WHITE : new Color(247, 248, 244));
            g.fillRect(0, y, WIDTH, row.height);
            g.setColor(Color.DARK_GRAY);
            g.setFont(BODY);
            x = 0;
            for (int col = 0; col < row.lines.size(); col++) {
                int lineY = y + 14;
                for (String line : row.lines.get(col)) {
                    int lineX = col == 5 ? x + COLUMN_WIDTHS[col] - 6 - g.getFontMetrics().stringWidth(line) : x + 5;
                    g.drawString(line, lineX, lineY);
                    lineY += 12;
                }
                x += COLUMN_WIDTHS[col];
            }
            y += row.height;
            g.setColor(StaffStyles.LINE);
            g.drawLine(0, y, WIDTH, y);
        }
        g.setColor(StaffStyles.MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g.drawLine(0, 699, WIDTH, 699);
        g.drawString("Operational report. Payment methods are simulated; this is not a tax invoice.", 0, 714);
        g.drawString("Report ID: " + reportId, 0, 730);
        String page = "Page " + (index + 1) + " of " + pages.size();
        g.drawString(page, WIDTH - g.getFontMetrics().stringWidth(page), 730);
    }

    private static void centered(Graphics2D g, String text, int y, Font font) {
        g.setFont(font);
        g.drawString(text, (WIDTH - g.getFontMetrics().stringWidth(text)) / 2, y);
    }

    private static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : String.valueOf(text).replace('\r', ' ').split("\n", -1)) {
            String remaining = paragraph;
            while (fm.stringWidth(remaining) > width) {
                int end = remaining.length();
                while (end > 1 && fm.stringWidth(remaining.substring(0, end)) > width) end--;
                int space = remaining.lastIndexOf(' ', end);
                if (space > 0) end = space;
                lines.add(remaining.substring(0, end));
                remaining = remaining.substring(end).stripLeading();
            }
            lines.add(remaining);
        }
        return lines;
    }

    private static final class Row {
        final List<List<String>> lines = new ArrayList<>();
        final int height;
        Row(String[] cells, FontMetrics fm) {
            int count = 1;
            for (int i = 0; i < cells.length; i++) {
                List<String> cell = wrap(cells[i], fm, COLUMN_WIDTHS[i] - 12);
                lines.add(cell);
                count = Math.max(count, cell.size());
            }
            height = count * 12 + 12;
        }
    }
}
