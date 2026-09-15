package gui.customer;

import javax.swing.*;
import model.*;
import java.lang.reflect.*;

public final class CheckoutValidationTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                CartPanel panel = new CartPanel(new User(1, "customer123", "test@example.com", "unused",
                        UserRole.CUSTOMER, true, AccountStatus.ACTIVE), () -> {});
                JTextField name = field(panel, "fullName");
                JTextField contact = field(panel, "contact");
                JTextField reference = field(panel, "qrReference");
                check(name.getText().isEmpty(), "Do not use a username as the full name");
                name.setText("Maria Dela Cruz");
                name.selectAll(); name.replaceSelection("Maria123!");
                check(name.getText().equals("Maria Dela Cruz"), "Invalid name paste preserves previous value");
                name.selectAll(); name.replaceSelection("A".repeat(37));
                check(name.getText().equals("Maria Dela Cruz"), "Name length capped at 36");
                contact.setText("9123456789");
                contact.selectAll(); contact.replaceSelection("+639123456789");
                check(contact.getText().equals("9123456789"), "Country code is not typed into contact");
                contact.selectAll(); contact.replaceSelection("91234abc");
                check(contact.getText().equals("9123456789"), "Contact rejects letters atomically");
                contact.setText("");
                check(contact.getText().isEmpty(), "Deleting remains possible");
                check(!CheckoutValidation.contactError("123").isEmpty(), "Short contacts rejected at submission");
                check(!CheckoutValidation.nameError("   ").isEmpty(), "Blank name rejected");
                reference.setText("123456");
                reference.selectAll(); reference.replaceSelection("AB-123456");
                check(reference.getText().equals("123456"), "Reference rejects letters and punctuation");
                reference.selectAll(); reference.replaceSelection("1".repeat(25));
                check(reference.getText().equals("123456"), "Reference capped at 24 digits");
                check(!CheckoutValidation.referenceError("12345").isEmpty(), "Reference minimum enforced");
                contact.setText("9123456789");
                Method customer = CartPanel.class.getDeclaredMethod("validateCustomerDetails"); customer.setAccessible(true);
                Method payment = CartPanel.class.getDeclaredMethod("validatePaymentDetails"); payment.setAccessible(true);
                customer.invoke(panel); payment.invoke(panel);
                Field receiptField = CartPanel.class.getDeclaredField("receipt"); receiptField.setAccessible(true);
                check(((JTextArea) receiptField.get(panel)).getText().contains("+639123456789"), "Receipt includes country code");
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("PASS: checkout name, contact and reference restrictions, paste handling, submission and +63 receipt");
    }
    private static JTextField field(CartPanel panel, String name) throws Exception {
        Field f = CartPanel.class.getDeclaredField(name); f.setAccessible(true); return (JTextField) f.get(panel);
    }
    private static void check(boolean value, String description) { if (!value) throw new AssertionError(description); }
}
