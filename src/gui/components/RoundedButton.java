package gui.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class RoundedButton extends JButton {

    private Color bgColor;
    private Color textColor;
    private Color hoverColor;
    private boolean hovered = false;

    public RoundedButton(String text, Color bgColor, Color textColor) {
        super(text);
        this.bgColor = bgColor;
        this.textColor = textColor;
        this.hoverColor = bgColor.darker();
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

    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
        repaint();
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
    }

    //PANG MANUAL TO LALAGAY NIYO ANO KULAY DYAN SA HOVERCOLOR
    public void setHoverColor(Color hoverColor) {
        this.hoverColor = hoverColor;
        repaint();
    }

    public Color getBgColor() { return bgColor; }
    public Color getTextColor() { return textColor; }
    public Color getHoverColor() { return hoverColor; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fill = isEnabled() && (hovered || getModel().isRollover()) ? hoverColor : bgColor;
        if (isEnabled() && getModel().isPressed()) fill = fill.darker();
        if (!isEnabled()) g2.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

        g2.setColor(textColor);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), x, y);

        if (isFocusOwner() && isEnabled()) {
            g2.setColor(textColor);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, getHeight() - 9, getHeight() - 9);
        }

        g2.dispose();
    }
}
