package backend;

import java.security.SecureRandom;

public class OtpUtil {
    public static String generateOTP() {
        SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000); // Generates a 6-digit code
        return String.valueOf(number);
    }
}