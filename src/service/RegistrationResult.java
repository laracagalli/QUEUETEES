package service;

import model.User;

public class RegistrationResult {

    private final AuthStatus status;
    private final String message;
    private final User user;

    private RegistrationResult(AuthStatus status, String message, User user) {
        this.status = status;
        this.message = message;
        this.user = user;
    }

    public static RegistrationResult success(User user) {
        return new RegistrationResult(AuthStatus.SUCCESS, "Registration successful.", user);
    }

    public static RegistrationResult failure(AuthStatus status, String message) {
        return new RegistrationResult(status, message, null);
    }

    public AuthStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public boolean isSuccess() {
        return status == AuthStatus.SUCCESS;
    }
}
