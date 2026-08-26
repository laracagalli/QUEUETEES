package backend;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    public static void sendOtpEmail(String recipientEmail, String otp) {
        // 1. SMTP Server Configuration
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com"); // Example: Gmail SMTP
        props.put("mail.smtp.port", "587");

        String senderEmail = "your-system-email@gmail.com";
        String senderPassword = "your-app-password"; // Use App Passwords for security

        // 2. Create Session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // 3. Create and Send Message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("Your Registration Verification Code");
            message.setText("Your OTP verification code is: " + otp + "\n\nThis code will expire in 5 minutes.");

            Transport.send(message);
            System.out.println("OTP email sent successfully!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}