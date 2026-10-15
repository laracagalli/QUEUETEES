package service;

import java.time.*;

/** Shared offline syntax checks. OTP subsequently verifies ownership of the mailbox. */
public final class RegistrationValidation {
    private RegistrationValidation() { }
    public static String contactError(String value) {
        return value!=null && value.matches("9[0-9]{9}") ? "" : "Enter 10 digits starting with 9 after +63.";
    }
    public static boolean validEmail(String value) {
        if (value == null || !value.equals(value.trim()) || value.length() > 254) return false;
        String[] parts=value.split("@",-1);
        if(parts.length!=2 || parts[0].isEmpty() || parts[0].length()>64 || parts[0].startsWith(".")
                || parts[0].endsWith(".") || parts[0].contains("..")
                || !parts[0].matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+")) return false;
        String[] domain=parts[1].split("\\.",-1);
        if(domain.length<2 || !domain[domain.length-1].matches("[A-Za-z]{2,63}"))return false;
        for(String label:domain)if(!label.matches("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"))return false;
        return true;
    }
    public static String birthdayError(LocalDate birthday) {
        if(birthday==null)return "Please enter your birthday.";
        if(birthday.isAfter(LocalDate.now()))return "Birthday cannot be in the future.";
        int age=Period.between(birthday,LocalDate.now()).getYears();
        return age<18 ? "You must be at least 18 years old to register." : age>120 ? "Age must not exceed 120 years." : "";
    }
    public static String addressError(String address) {
        return address==null || address.trim().length()<8 || address.length()>400 || address.matches("(?s).*\\p{Cntrl}.*")
            || address.chars().filter(c->c==',').count()<3 ? "Provide house/street, barangay, city and province." : "";
    }
}
