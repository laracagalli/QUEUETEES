package gui.staff;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import model.Order;
import model.OrderStatus;

/** Shared queue meaning and visual language; positions never depend on table sorting. */
final class StaffQueuePresentation {
    private StaffQueuePresentation() { }
    static Color statusColor(String label) {
        if (OrderStatus.CONFIRMED.getLabel().equals(label)) return new Color(139, 91, 15);
        if (OrderStatus.PREPARING.getLabel().equals(label)) return new Color(34, 91, 157);
        if (OrderStatus.READY_FOR_PICKUP.getLabel().equals(label)) return new Color(29, 113, 75);
        return new Color(100, 78, 138);
    }
    static Map<Integer, Integer> waitingPositions(List<Order> orders) {
        Map<Integer, Integer> positions = new LinkedHashMap<>();
        orders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
                .sorted(java.util.Comparator.comparingInt(Order::getQueueNumber))
                .forEach(o -> positions.put(o.getId(), positions.size() + 1));
        return positions;
    }
    static String nextMessage(List<Order> orders) {
        return orders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
                .min(java.util.Comparator.comparingInt(Order::getQueueNumber))
                .map(o -> String.format("Next to prepare: Q-%03d  |  Waiting #1", o.getQueueNumber()))
                .orElse(orders.isEmpty() ? "No active orders. New orders will appear here." : "No orders waiting to start. Continue preparing or completing active orders.");
    }
    static JPanel legend() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        panel.setOpaque(false);
        for (OrderStatus status : OrderStatus.values()) {
            String label = status.getLabel();
            panel.add(StaffStyles.label("● " + label, 11, true, statusColor(label)));
        }
        return panel;
    }
    static JTable table(javax.swing.table.TableModel model, String emptyTitle, String emptyHint) {
        return new JTable(model) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (getRowCount() != 0) return;
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean filtered = getModel().getRowCount() > 0;
                String title = filtered ? "No matching orders" : emptyTitle;
                String hint = filtered ? "Clear or change your search to see more orders." : emptyHint;
                int y = Math.max(65, getHeight() / 2 - 12);
                g.setFont(new Font("Segoe UI", Font.BOLD, 17)); g.setColor(StaffStyles.FOREST);
                g.drawString(title, Math.max(12, (getWidth() - g.getFontMetrics().stringWidth(title)) / 2), y);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12)); g.setColor(StaffStyles.MUTED);
                g.drawString(hint, Math.max(12, (getWidth() - g.getFontMetrics().stringWidth(hint)) / 2), y + 25);
                g.dispose();
            }
        };
    }
    static void style(JTable table, int statusColumn, boolean positions) {
        table.setRowHeight(48);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setShowVerticalLines(false);
        table.setGridColor(StaffStyles.LINE);
        table.setSelectionBackground(new Color(222, 232, 220));
        table.setSelectionForeground(new Color(28, 31, 27));
        DefaultTableCellRenderer base = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                putClientProperty("html.disable", true);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setBackground(selected ? t.getSelectionBackground() : row % 2 == 0 ? StaffStyles.PAPER : new Color(245, 247, 241));
                setForeground(t.getSelectionForeground());
                setFont(t.getFont());
                setHorizontalAlignment(LEFT);
                if (t.convertColumnIndexToModel(col) == statusColumn) {
                    setForeground(statusColor(String.valueOf(value)));
                    setFont(t.getFont().deriveFont(Font.BOLD));
                } else if (positions && t.convertColumnIndexToModel(col) == 0) {
                    setHorizontalAlignment(CENTER);
                    setText(value == null ? "—" : value.toString());
                    setFont(t.getFont().deriveFont(Font.BOLD));
                    setForeground(value != null && value.equals(1) ? new Color(139, 91, 15) : StaffStyles.MUTED);
                }
                setToolTipText(value == null ? "Already in progress; no longer waiting to start" : String.valueOf(value));
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, base);
        table.setDefaultRenderer(Integer.class, base);
        table.getColumnModel().getColumn(statusColumn).setMinWidth(145);
        if (positions) {
            table.getColumnModel().getColumn(0).setMinWidth(85);
            table.getColumnModel().getColumn(0).setMaxWidth(100);
            table.getTableHeader().setToolTipText("Waiting # is preparation order. Q-number is the permanent order ticket. Sorting does not change priority.");
        }
    }
}
