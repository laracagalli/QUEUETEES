package gui.components;

import java.awt.*;
import javax.swing.*;

public class OptionPaneAlignmentTest {
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents())
            if (child instanceof Container) layout((Container) child);
    }
    private static JLabel label(Container container, boolean icon) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel && (((JLabel) child).getIcon() != null) == icon)
                return (JLabel) child;
            if (child instanceof Container) {
                JLabel found = label((Container) child, icon);
                if (found != null) return found;
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppTheme.install();
            for (int type : new int[] {JOptionPane.QUESTION_MESSAGE, JOptionPane.WARNING_MESSAGE,
                    JOptionPane.ERROR_MESSAGE, JOptionPane.INFORMATION_MESSAGE}) {
                JOptionPane pane = new JOptionPane("Log out of QueueTees?", type, JOptionPane.YES_NO_OPTION);
                pane.setSize(390, 150);
                layout(pane);
                // No AppTheme.apply call: alignment must be correct on initial creation.
                JLabel icon = label(pane, true), text = label(pane, false);
                if (icon == null || text == null || icon.getVerticalAlignment() != SwingConstants.CENTER)
                    throw new AssertionError("Dialog icon not aligned before first paint");
                Point iconCenter = SwingUtilities.convertPoint(icon, 0, icon.getHeight() / 2, pane);
                Point textCenter = SwingUtilities.convertPoint(text, 0, text.getHeight() / 2, pane);
                if (Math.abs(iconCenter.y - textCenter.y) > 1)
                    throw new AssertionError("Message and icon have different vertical centers");
            }
            System.out.println("PASS: all option-pane message types align before first paint");
        });
    }
}
