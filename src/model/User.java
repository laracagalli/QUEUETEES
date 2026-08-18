package model;

public class User {
    private final int id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private boolean emailVerified;
    private AccountStatus status;

    public User(int id,
                String username,
                String email,
                String passwordHash,
                UserRole role,
                boolean emailVerified,
                AccountStatus status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailVerified = emailVerified;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
