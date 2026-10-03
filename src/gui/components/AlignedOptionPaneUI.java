package gui.components;

import java.awt.Component;
import java.awt.Container;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;

/** Aligns the dialog icon with the message area during UI creation, before painting. */
public final class AlignedOptionPaneUI extends BasicOptionPaneUI {
    public static ComponentUI createUI(JComponent component) {
        return new AlignedOptionPaneUI();
    }

    @Override protected Container createMessageArea() {
        Container area = super.createMessageArea();
        alignIcons(area);
        return area;
    }

    private static void alignIcons(Component component) {
        if (component instanceof JLabel && ((JLabel) component).getIcon() != null)
            ((JLabel) component).setVerticalAlignment(SwingConstants.CENTER);
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) alignIcons(child);
    }
}
