package service;

import model.User;

public class RegistrationResult {
    private final boolean success;
    private final String message;
    private final User user;

    private RegistrationResult(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public static RegistrationResult success(User user) {
        return new RegistrationResult(true, "Registration successful.", user);
    }

    public static RegistrationResult failure(String message) {
        return new RegistrationResult(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public User getUser() { return user; }
}
