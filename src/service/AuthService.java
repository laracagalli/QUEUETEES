package service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import model.AccountStatus;
import model.User;
import model.UserRole;
import repository.UserRepository;

public class AuthService {
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordResetService = new PasswordResetService(userRepository,
                backend.EmailService::sendPasswordResetEmail);
    }

    public PasswordResetService passwordResets() {
        return passwordResetService;
    }

    public LoginResult login(String identifier, char[] passwordChars) {
        String cleanIdentifier = identifier == null ? "" : identifier.trim();
        String password = passwordChars == null ? "" : new String(passwordChars);

        try {
            if (cleanIdentifier.isEmpty() || password.isEmpty()) {
                return LoginResult.failure(
                        AuthStatus.EMPTY_FIELDS,
                        "Please enter your email/username and password.");
            }

            Optional<User> optionalUser = userRepository.findByEmailOrUsername(cleanIdentifier);
            if (optionalUser == null || !optionalUser.isPresent()) {
                return invalidCredentials();
            }

            User user = optionalUser.get();

            if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                return invalidCredentials();
            }

            if (user.getStatus() == AccountStatus.SUSPENDED) {
                return LoginResult.failure(AuthStatus.ACCOUNT_SUSPENDED, "This account is suspended.");
            }

            if (user.getStatus() == AccountStatus.BANNED) {
                return LoginResult.failure(AuthStatus.ACCOUNT_BANNED, "This account is banned.");
            }

            if (user.getStatus() == AccountStatus.REJECTED) {
                return LoginResult.failure(AuthStatus.ACCOUNT_REJECTED, "This account registration was rejected.");
            }

            // QueueTees requirement: customers must verify their email before customer
            // access.
            if (user.getRole() == UserRole.CUSTOMER && !user.isEmailVerified()) {
                return LoginResult.failure(
                        AuthStatus.EMAIL_NOT_VERIFIED,
                        "Please verify your email before logging in.",
                        user);
            }

            // QueueTees requirement: staff must verify their email before logging in.
            if (user.getRole() == UserRole.STAFF && !user.isEmailVerified()) {
                return LoginResult.failure(
                        AuthStatus.EMAIL_NOT_VERIFIED,
                        "Please verify your email before logging in.",
                        user);
            }

            // QueueTees requirement: staff must be approved by an administrator first.
            if (user.getRole() == UserRole.STAFF && user.getStatus() != AccountStatus.ACTIVE) {
                return LoginResult.failure(
                        AuthStatus.STAFF_NOT_APPROVED,
                        "Your staff account is still waiting for administrator approval.");
            }

            if (user.getStatus() != AccountStatus.ACTIVE) {
                return LoginResult.failure(
                        AuthStatus.INVALID_CREDENTIALS,
                        "This account cannot log in right now.");
            }

            ActivityLogger.setActor(user);
            ActivityLogger.record(user, "Logged in", "Signed in successfully");
            return LoginResult.success(user);
        } finally {
            if (passwordChars != null) {
                java.util.Arrays.fill(passwordChars, '\0');
            }
        }
    }

    private LoginResult invalidCredentials() {
        return LoginResult.failure(
                AuthStatus.INVALID_CREDENTIALS,
                "Invalid email/username or password.");
    }

    public void verifyEmail(User user) {
        user.setEmailVerified(true);
    }

    // ==========================================
    // NEW METHOD: Persist user profile updates
    // ==========================================
    public void updateUser(User user) {
        if (user != null) {
            userRepository.save(user);
            ActivityLogger.record(user, "Profile updated", "Updated account profile");
        }
    }

    public RegistrationResult registerCustomer(
            String email, String fullname, String username, char[] passwordChars,
            String address, String contactnum, String gender, LocalDate birthday) {
        return register(email, fullname, username, passwordChars, address, contactnum, gender, birthday,
                UserRole.CUSTOMER);
    }

    public RegistrationResult registerStaff(
            String email, String fullname, String username, char[] passwordChars,
            String address, String contactnum, String gender, LocalDate birthday) {
        return register(email, fullname, username, passwordChars, address, contactnum, gender, birthday,
                UserRole.STAFF);
    }

    private void requireAdministrator(User actor) {
        if (actor == null || actor.getRole() != UserRole.ADMIN || actor.getStatus() != AccountStatus.ACTIVE
                || userRepository.findByEmailOrUsername(actor.getEmail()).orElse(null) != actor)
            throw new IllegalStateException("An active administrator account is required.");
    }

    public java.util.List<User> getStaffApplications(User actor) {
        requireAdministrator(actor);
        return userRepository.findAll().stream().filter(u -> u.getRole() == UserRole.STAFF)
                .collect(java.util.stream.Collectors.toList());
    }

    // ==========================================
    // NEW METHOD: Fetch customers for Admin UI
    // ==========================================
    public java.util.List<User> getCustomers(User actor) {
        requireAdministrator(actor);
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .collect(java.util.stream.Collectors.toList());
    }

    public void changePassword(User user, char[] currentPassword, char[] newPassword, char[] confirmPassword) {
        String current = currentPassword == null ? "" : new String(currentPassword);
        String next = newPassword == null ? "" : new String(newPassword);
        String confirm = confirmPassword == null ? "" : new String(confirmPassword);
        try {
            if (current.isEmpty() || next.isEmpty() || confirm.isEmpty())
                throw new IllegalArgumentException("Please fill in all password fields.");
            if (!PasswordUtil.verifyPassword(current, user.getPasswordHash()))
                throw new IllegalArgumentException("Current password is incorrect.");
            String validationMessage = PasswordUtil.getPasswordValidationMessage(next);
            if (validationMessage != null)
                throw new IllegalArgumentException(validationMessage + ".");
            if (!next.equals(confirm))
                throw new IllegalArgumentException("New passwords do not match.");
            user.setPasswordHash(PasswordUtil.hashPassword(next));
            ActivityLogger.record(user, "Password changed", "Changed account password");
        } finally {
            if (currentPassword != null) java.util.Arrays.fill(currentPassword, '\0');
            if (newPassword != null) java.util.Arrays.fill(newPassword, '\0');
            if (confirmPassword != null) java.util.Arrays.fill(confirmPassword, '\0');
        }
    }

    public void suspendUser(User actor, int targetId, boolean suspend) {
        requireAdministrator(actor);
        User target = userRepository.findAll().stream().filter(u -> u.getId() == targetId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (target.getRole() == UserRole.ADMIN)
            throw new IllegalStateException("Administrator accounts cannot be suspended.");
        if (suspend && target.getStatus() == AccountStatus.SUSPENDED)
            throw new IllegalStateException("This account is already suspended.");
        if (!suspend && target.getStatus() != AccountStatus.SUSPENDED)
            throw new IllegalStateException("This account is not suspended.");
        target.setStatus(suspend ? AccountStatus.SUSPENDED : AccountStatus.ACTIVE);
        ActivityLogger.record(actor, suspend ? "Account suspended" : "Account reactivated", target.getUsername() + " (#" + targetId + ")");
    }

    public void reviewStaff(User actor, int staffId, boolean approve) {
        requireAdministrator(actor);
        User staff = userRepository.findAll().stream().filter(u -> u.getId() == staffId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Staff account was not found."));
        if (staff.getRole() != UserRole.STAFF || staff.getStatus() != AccountStatus.PENDING_APPROVAL)
            throw new IllegalStateException("Only pending staff applications can be reviewed. Refresh and try again.");
        staff.setStatus(approve ? AccountStatus.ACTIVE : AccountStatus.REJECTED);
        ActivityLogger.record(actor, approve ? "Staff approved" : "Staff rejected", staff.getUsername() + " (#" + staffId + ")");
        // Send email notification to staff in background
        final String staffEmail = staff.getEmail();
        final String staffUsername = staff.getUsername();
        new Thread(() -> {
            try {
                if (approve) backend.EmailService.sendStaffApprovalEmail(staffEmail, staffUsername);
                else backend.EmailService.sendStaffRejectionEmail(staffEmail, staffUsername);
            } catch (Exception ignored) { }
        }, "staff-notify").start();
    }

    private synchronized RegistrationResult register(
            String email,
            String fullname,
            String username,
            char[] passwordChars,
            String address,
            String contactnum,
            String gender,
            LocalDate birthday, UserRole role) {

        String cleanEmail = email == null ? "" : email.trim();
        String cleanUsername = username == null ? "" : username.trim();
        String cleanFullname = fullname == null ? "" : fullname.trim();
        String password = passwordChars == null ? "" : new String(passwordChars);

        try {
            if (cleanEmail.isEmpty() || cleanUsername.isEmpty() || cleanFullname.isEmpty() || password.isEmpty()) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS, "Please fill in all required fields.");
            }

            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                return RegistrationResult.failure(AuthStatus.INVALID_EMAIL, "Please enter a valid email address.");
            }

            if (userRepository.findByEmailOrUsername(cleanEmail).isPresent()) {
                return RegistrationResult.failure(AuthStatus.DUPLICATE_EMAIL, "That email is already registered.");
            }

            if (userRepository.findByEmailOrUsername(cleanUsername).isPresent()) {
                return RegistrationResult.failure(AuthStatus.DUPLICATE_USERNAME, "That username is already taken.");
            }

            String passwordMessage = PasswordUtil.getPasswordValidationMessage(password);
            if (passwordMessage != null) {
                return RegistrationResult.failure(AuthStatus.WEAK_PASSWORD, passwordMessage + ".");
            }

            if (gender == null || gender.isEmpty()) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS, "Please select a gender.");
            }

            if (birthday == null) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS, "Please enter your birthday.");
            }

            int age = Period.between(birthday, LocalDate.now()).getYears();
            if (age < 18) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS,
                        "You must be at least 18 years old to register.");
            }

            if (role == UserRole.STAFF && (address == null || address.trim().isEmpty()
                    || contactnum == null || !contactnum.trim().matches("[0-9]{10}"))) {
                return RegistrationResult.failure(AuthStatus.EMPTY_FIELDS,
                        "Enter your address and a 10-digit contact number.");
            }

            int newId = userRepository.findAll().stream().mapToInt(User::getId).max().orElse(0) + 1;
            User newUser = new User(
                    newId,
                    cleanUsername,
                    cleanEmail,
                    PasswordUtil.hashPassword(password),
                    role,
                    false,
                    role == UserRole.STAFF ? AccountStatus.PENDING_APPROVAL : AccountStatus.ACTIVE,
                    cleanFullname, address == null ? "" : address.trim(),
                    contactnum == null ? "" : contactnum.trim(), gender, birthday);

            userRepository.save(newUser);
            return RegistrationResult.success(newUser);

        } finally {
            if (passwordChars != null) {
                java.util.Arrays.fill(passwordChars, '\0');
            }
        }
    }
}
