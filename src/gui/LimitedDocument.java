package gui;

import java.awt.Toolkit;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class LimitedDocument extends PlainDocument {

    private final int maxLength;
    private final boolean digitsOnly;
    private final String fieldName;
    private final Runnable afterWarning;

    public LimitedDocument(
            int maxLength,
            boolean digitsOnly,
            String fieldName,
            Runnable afterWarning) {

        this.maxLength = maxLength;
        this.digitsOnly = digitsOnly;
        this.fieldName = fieldName;
        this.afterWarning = afterWarning;
    }

    @Override
    public void insertString(
            int offset,
            String text,
            AttributeSet attributes)
            throws BadLocationException {

        if (text == null) {
            return;
        }

        // Numbers only
        if (digitsOnly && !text.matches("\\d+")) {

            warning(
                    "Only numbers are allowed in "
                    + fieldName + "."
            );

            return;
        }

        // Character limit
        if (getLength() + text.length() > maxLength) {

            String unit;

            if (digitsOnly) {
                unit = " digits.";
            } else {
                unit = " characters.";
            }

            warning(
                    fieldName
                    + " can only contain up to "
                    + maxLength
                    + unit
            );

            return;
        }

        super.insertString(offset, text, attributes);
    }

    private void warning(String message) {

        Toolkit.getDefaultToolkit().beep();

        JOptionPane.showMessageDialog(
                null,
                message,
                "Input Warning",
                JOptionPane.WARNING_MESSAGE
        );

        if (afterWarning != null) {
            SwingUtilities.invokeLater(afterWarning);
        }
    }
}