package model;

public class User {
    private final int id;
    private final String username;
    private final String email;
    private volatile String passwordHash;
    private final UserRole role;
    private boolean emailVerified;
    private AccountStatus status;
    private String fullName = "";
    private String address = "";
    private String contactNumber = "";
    private String gender = "";
    private java.time.LocalDate birthday;
    private final java.time.LocalDateTime registeredAt = java.time.LocalDateTime.now();

    public User(int id, String username, String email, String passwordHash, UserRole role,
                boolean emailVerified, AccountStatus status, String fullName, String address,
                String contactNumber, String gender, java.time.LocalDate birthday) {
        this(id, username, email, passwordHash, role, emailVerified, status);
        this.fullName = fullName;
        this.address = address;
        this.contactNumber = contactNumber;
        this.gender = gender;
        this.birthday = birthday;
    }

    public String getFullName() { return fullName; }
    public String getAddress() { return address; }
    public String getContactNumber() { return contactNumber; }
    public String getGender() { return gender; }
    public java.time.LocalDate getBirthday() { return birthday; }
    public java.time.LocalDateTime getRegisteredAt() { return registeredAt; }

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

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = java.util.Objects.requireNonNull(passwordHash);
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
