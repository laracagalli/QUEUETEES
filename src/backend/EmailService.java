package backend;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;


public class EmailService {

    public static void sendPasswordResetEmail(String email, String code) throws MessagingException {
        sendCode(email, code, "QueueTees password reset code", "Your password reset code is: ", 10);
    }

    private static void sendCode(String email, String code, String subject, String introduction, int minutes)
            throws MessagingException {
        String sender = System.getenv("QUEUETEES_SMTP_EMAIL");
        String password = System.getenv("QUEUETEES_SMTP_PASSWORD");
        if (sender == null || sender.isBlank() || password == null || password.isBlank())
            throw new MessagingException("Email delivery is not configured.");
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, password);
            }
        });
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        message.setSubject(subject);
        message.setText(introduction + code
                + "\n\nThis code expires in " + minutes + " minutes. If you did not request this, ignore this email.");
        Transport.send(message);
    }

    public static void sendOtpEmail(String email, String code) throws MessagingException {
        sendCode(email, code, "Your Registration Verification Code",
                "Your email verification code is: ", 5);
    }
}
