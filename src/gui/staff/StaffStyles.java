package gui.staff;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/** Consistent staff controls and clearly non-editable display text. */
final class StaffStyles {
    static final Color FOREST = new Color(55, 70, 56);
    static final Color PAPER = new Color(252, 252, 247);
    static final Color MUTED = new Color(99, 106, 96);
    static final Color LINE = new Color(218, 220, 209);
    private StaffStyles() { }

    static gui.components.RoundedButton button(String text) {
        return button(text, false);
    }

    static gui.components.RoundedButton lightButton(String text) {
        return button(text, true);
    }

    private static gui.components.RoundedButton button(String text, boolean light) {
        Color ink = new Color(28, 31, 27);
        gui.components.RoundedButton button = new gui.components.RoundedButton(text, light ? Color.WHITE : ink, light ? ink : Color.WHITE);
        button.setHoverColor(light ? new Color(228, 228, 228) : new Color(65, 65, 65));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(Math.max(130, text.length() * 8 + 28), 38));
        button.setMinimumSize(new Dimension(100, 38));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static int confirm(Component parent, String message, String title, int optionType) {
        return dialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, true);
    }

    static void showMessage(Component parent, String message) {
        showMessage(parent, message, "QueueTees", JOptionPane.INFORMATION_MESSAGE);
    }

    static void showMessage(Component parent, String message, String title, int messageType) {
        dialog(parent, message, title, messageType, false);
    }

    private static int dialog(Component parent, String message, String title, int messageType, boolean confirmation) {
        gui.components.RoundedButton accept = button(confirmation ? "Yes" : "OK");
        gui.components.RoundedButton cancel = lightButton("Cancel");
        Object[] options = confirmation ? new Object[]{cancel, accept} : new Object[]{accept};
        JOptionPane pane = new JOptionPane(message, messageType, JOptionPane.DEFAULT_OPTION, null, options, accept);
        JDialog dialog = pane.createDialog(parent, title);
        int[] result = {JOptionPane.CLOSED_OPTION};
        accept.addActionListener(e -> { result[0] = JOptionPane.YES_OPTION; dialog.dispose(); });
        cancel.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(accept);
        dialog.setVisible(true);
        dialog.dispose();
        return result[0];
    }

    static void readOnly(JTextComponent text) {
        text.setEditable(false);
        text.setFocusable(false);
        text.setCursor(Cursor.getDefaultCursor());
        text.getCaret().setVisible(false);
    }

    static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        // User-entered names must remain plain text, including names beginning with HTML.
        label.putClientProperty("html.disable", true);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(PAPER);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        return panel;
    }
}
