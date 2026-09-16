package gui.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/** Cream and green dropdown retaining standard Swing keyboard selection. */
public final class RoundedComboBox<E> extends JComboBox<E> {
    private static final Color GREEN = new Color(55, 70, 56);
    private static final Color CREAM = new Color(246, 247, 240);
    private static final Color HOVER = new Color(230, 236, 224);
    private boolean hovered;

    public RoundedComboBox(E[] values) {
        super(values);
        setFont(new Font("Fira Code", Font.PLAIN, 11));
        setForeground(GREEN);
        setBackground(CREAM);
        setOpaque(false);
        setBorder(new EmptyBorder(3, 8, 3, 5));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setFont(RoundedComboBox.this.getFont());
                setBorder(new EmptyBorder(10, 12, 10, 12));
                setForeground(isEnabled() ? GREEN : Color.GRAY);
                setBackground(selected ? HOVER : CREAM);
                setOpaque(index >= 0);
                return this;
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    @Override public void updateUI() {
        setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    @Override protected void paintComponent(Graphics graphics) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(isEnabled() ? GREEN : Color.GRAY);
                        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int x = getWidth()/2, y = getHeight()/2;
                        g.drawLine(x-4, y-2, x, y+2);
                        g.drawLine(x, y+2, x+4, y-2);
                        g.dispose();
                    }
                };
                arrow.setBorder(BorderFactory.createEmptyBorder());
                arrow.setContentAreaFilled(false);
                arrow.setOpaque(false);
                arrow.setFocusable(false);
                arrow.setPreferredSize(new Dimension(30, 30));
                arrow.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered=true; RoundedComboBox.this.repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered=false; RoundedComboBox.this.repaint(); }
                });
                return arrow;
            }
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean focus) { }
            @Override protected ComboPopup createPopup() {
                BasicComboPopup popup = (BasicComboPopup) super.createPopup();
                popup.setBorder(BorderFactory.createLineBorder(new Color(185, 197, 177)));
                popup.getList().setBackground(CREAM);
                return popup;
            }
        });
    }

    @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(190, size.width + 16), Math.max(40, size.height));
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isEnabled() && (hovered || isPopupVisible()) ? HOVER : CREAM);
        g.fillRoundRect(1, 1, getWidth()-3, getHeight()-3, 18, 18);
        g.setColor(hasFocus() || isPopupVisible() ? GREEN : new Color(201, 211, 193));
        g.setStroke(new BasicStroke(hasFocus() ? 2f : 1f));
        g.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 18, 18);
        g.dispose();
        super.paintComponent(graphics);
    }
}
