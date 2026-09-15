package gui.customer;

import java.util.function.Consumer;
import javax.swing.JTextField;
import javax.swing.text.*;

/** Signup-compatible input limits, applied atomically to typing and pasted replacements. */
final class CheckoutValidation {
    private CheckoutValidation() { }

    static void restrict(JTextField field, int limit, String allowed, String message, Consumer<String> feedback) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributes)
                    throws BadLocationException {
                replace(fb, offset, 0, text, attributes);
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attributes)
                    throws BadLocationException {
                String replacement = text == null ? "" : text;
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String candidate = current.substring(0, offset) + replacement + current.substring(offset + length);
                if (candidate.length() > limit || !candidate.matches(allowed)) {
                    feedback.accept(message);
                    return;
                }
                super.replace(fb, offset, length, replacement, attributes);
                feedback.accept("");
            }
            @Override public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                replace(fb, offset, length, "", null);
            }
        });
    }

    static String nameError(String value) {
        String name = value.trim();
        return name.isEmpty() ? "Full name is required."
                : value.length() > 36 || !name.matches("[A-Za-z ]+") ? "Use letters and spaces only (maximum 36 characters)." : "";
    }
    static String contactError(String value) {
        return value.matches("[0-9]{10}") ? "" : "Enter exactly 10 digits after +63.";
    }
    static String referenceError(String value) {
        return value.matches("[0-9]{6,24}") ? "" : "Enter a 6-24 digit reference number.";
    }
}
