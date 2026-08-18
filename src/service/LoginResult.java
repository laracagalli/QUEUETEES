package service;

import model.User;

public class LoginResult {
    private final AuthStatus status;
    private final String message;
    private final User user;

    private LoginResult(AuthStatus status, String message, User user) {
        this.status = status;
        this.message = message;
        this.user = user;
    }

    public static LoginResult success(User user) {
        return new LoginResult(AuthStatus.SUCCESS, "Login successful.", user);
    }

    public static LoginResult failure(AuthStatus status, String message) {
        return new LoginResult(status, message, null);
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
