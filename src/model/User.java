package model;

import java.time.LocalDate;

public class User {

    private final int id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final UserRole role;

    private final String fullName;
    private final String address;
    private final String contactNumber;
    private final String gender;
    private final LocalDate birthday;

    private boolean emailVerified;
    private AccountStatus status;

    // Constructor for the temporary test accounts in main.java
    public User(
            int id,
            String username,
            String email,
            String passwordHash,
            UserRole role,
            boolean emailVerified,
            AccountStatus status
    ) {
        this(
                id,
                username,
                email,
                passwordHash,
                role,
                emailVerified,
                status,
                "",
                "",
                "",
                "",
                null
        );
    }

    // Constructor for newly registered customers
    public User(
            int id,
            String username,
            String email,
            String passwordHash,
            UserRole role,
            boolean emailVerified,
            AccountStatus status,
            String fullName,
            String address,
            String contactNumber,
            String gender,
            LocalDate birthday
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailVerified = emailVerified;
        this.status = status;

        this.fullName = fullName;
        this.address = address;
        this.contactNumber = contactNumber;
        this.gender = gender;
        this.birthday = birthday;
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

    public String getFullName() {
        return fullName;
    }

    public String getAddress() {
        return address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }
}