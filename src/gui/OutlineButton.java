package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class OutlineButton extends JButton {

    private Color borderColor;
    private Color textColor;
    private Color bgColor;
    private boolean hovered = false;

    public OutlineButton(String text, Color borderColor, Color textColor) {
        super(text);
        this.borderColor = borderColor;
        this.textColor = textColor;
        this.bgColor = null; // null = transparent
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
    }

    // Set the hover fill color (optional, defaults to semi-transparent white)
    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill on hover
        if (hovered) {
            Color fill = (bgColor != null) ? bgColor : new Color(255, 255, 255, 40);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        }

        // Draw border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, getHeight(), getHeight());

        // Draw text
        g2.setColor(textColor);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), x, y);

        g2.dispose();
    }
}
