package gui.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import model.User;
import service.ActivityLogger;

/** Records named user commands, including commands in owned dialogs, without reading private form values. */
public final class ActivityTracking {
    private static boolean installed;
    private ActivityTracking() { }
    public static void track(JComponent root, User user) {
        root.putClientProperty("activity.actor", user);
        ActivityLogger.setActor(user);
        attach(root, user);
        if (installed) return;
        installed = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof ContainerEvent && event.getID() == ContainerEvent.COMPONENT_ADDED) {
                Component child = ((ContainerEvent) event).getChild();
                SwingUtilities.invokeLater(() -> attach(child, actor(child)));
            } else if (event instanceof WindowEvent && event.getID() == WindowEvent.WINDOW_OPENED) {
                Window window = ((WindowEvent) event).getWindow();
                attach(window, actor(window));
            }
        }, AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
    }
    private static User actor(Component component) {
        for (Component parent = component; parent != null;
                parent = parent instanceof Window ? ((Window) parent).getOwner() : parent.getParent()) {
            if (parent instanceof JComponent) {
                Object value = ((JComponent) parent).getClientProperty("activity.actor");
                if (value instanceof User) return (User) value;
            }
            if (parent instanceof RootPaneContainer) {
                Component content = ((RootPaneContainer) parent).getContentPane();
                if (content instanceof JComponent) {
                    Object value = ((JComponent) content).getClientProperty("activity.actor");
                    if (value instanceof User) return (User) value;
                }
            }
        }
        return null;
    }
    private static void attach(Component component, User user) {
        if (user == null) return;
        if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) component;
            String name = button.getText();
            if (name == null || name.isBlank() || name.equals("+") || name.equals("−")) name = button.getAccessibleContext().getAccessibleName();
            if (name != null && !name.isBlank() && !Boolean.TRUE.equals(button.getClientProperty("activity.tracked"))) {
                button.putClientProperty("activity.tracked", true);
                String command = name;
                button.addActionListener(e -> ActivityLogger.record(user, "UI command", command));
            }
        }
        if (component instanceof JTextField && Boolean.TRUE.equals(((JTextField)component).getClientProperty("searchField"))) {
            JTextField input = (JTextField) component;
            if (!Boolean.TRUE.equals(input.getClientProperty("activity.tracked"))) {
                input.putClientProperty("activity.tracked", true);
                Timer delay = new Timer(500, e -> {
                    if (input.isShowing()) ActivityLogger.record(user, "Search changed", input.getAccessibleContext().getAccessibleName());
                });
                delay.setRepeats(false);
                input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { if (input.isFocusOwner()) delay.restart(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { if (input.isFocusOwner()) delay.restart(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { if (input.isFocusOwner()) delay.restart(); }
                });
            }
        }
        if (component instanceof JTable) {
            JTable table = (JTable) component;
            if (!Boolean.TRUE.equals(table.getClientProperty("activity.tracked"))) {
                table.putClientProperty("activity.tracked", true);
                table.getTableHeader().addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        int column = table.columnAtPoint(e.getPoint());
                        if (column >= 0 && table.getRowSorter() instanceof javax.swing.table.TableRowSorter
                                && ((javax.swing.table.TableRowSorter<?>)table.getRowSorter()).isSortable(table.convertColumnIndexToModel(column)))
                            ActivityLogger.record(user, "Table sorted", "Column: " + table.getColumnName(column));
                    }
                });
            }
        }
        if (component instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) component;
            String name = combo.getAccessibleContext().getAccessibleName();
            if (name != null && name.startsWith("Filter") && !Boolean.TRUE.equals(combo.getClientProperty("activity.tracked"))) {
                combo.putClientProperty("activity.tracked", true);
                combo.addActionListener(e -> {
                    if (!Boolean.TRUE.equals(combo.getClientProperty("activity.suppress")) && (combo.isFocusOwner() || combo.isPopupVisible()))
                        ActivityLogger.record(user, "Filter changed", name + ": " + combo.getSelectedItem());
                });
            }
        }
        if (component instanceof JSpinner) {
            JSpinner spinner = (JSpinner) component;
            String name = spinner.getAccessibleContext().getAccessibleName();
            if (name != null && name.startsWith("Completion date") && !Boolean.TRUE.equals(spinner.getClientProperty("activity.tracked"))) {
                spinner.putClientProperty("activity.tracked", true);
                spinner.addChangeListener(e -> {
                    if (spinner.isShowing())
                        ActivityLogger.record(user, "Date filter changed", name + ": " + spinner.getValue());
                });
            }
        }
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) attach(child, user);
    }
}
