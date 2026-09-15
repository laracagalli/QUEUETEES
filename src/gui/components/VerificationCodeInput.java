package gui.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/** Six numeric cells, supporting both typing and pasting a complete email code. */
public final class VerificationCodeInput extends JPanel {
    private final JTextField[] fields = new JTextField[6];
    public VerificationCodeInput(Runnable submit) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 0)); setOpaque(false);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 58)); setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            JTextField field = new JTextField(); fields[i] = field;
            field.setPreferredSize(new Dimension(54, 58));
            field.setFont(new Font("Fira Code", Font.BOLD, 25));
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setBackground(new Color(246,248,243)); field.setForeground(new Color(28,31,27));
            field.getAccessibleContext().setAccessibleName("Verification digit " + (i+1));
            field.setBorder(BorderFactory.createLineBorder(new Color(145,155,145), 2, true));
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override public void insertString(FilterBypass fb, int offset, String value, AttributeSet attr) throws BadLocationException {
                    replace(fb, offset, 0, value, attr);
                }
                @Override public void replace(FilterBypass fb, int offset, int length, String value, AttributeSet attr) throws BadLocationException {
                    if (value == null || value.isEmpty()) { fb.replace(offset,length,"",attr); return; }
                    if (!value.matches("[0-9]+")) return;
                    if (value.length() == 6) {
                        SwingUtilities.invokeLater(() -> { for (int j=0;j<6;j++) fields[j].setText(value.substring(j,j+1)); fields[5].requestFocusInWindow(); });
                    } else if (value.length() == 1) {
                        fb.replace(0,fb.getDocument().getLength(),value,attr);
                        if(index<5) SwingUtilities.invokeLater(() -> fields[index+1].requestFocusInWindow());
                    }
                }
            });
            field.addActionListener(e -> submit.run());
            field.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if(e.getKeyCode()==KeyEvent.VK_BACK_SPACE && field.getText().isEmpty() && index>0) fields[index-1].requestFocusInWindow();
                }
            });
            add(field);
        }
    }
    public String getText() { StringBuilder code=new StringBuilder(); for(JTextField field:fields) code.append(field.getText()); return code.toString(); }
    public void clear() { for(JTextField field:fields) field.setText(""); }
    public void focusFirst() { fields[0].requestFocusInWindow(); }
}
