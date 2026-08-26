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

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResult login(String identifier, char[] passwordChars) {
        String cleanIdentifier = identifier == null ? "" : identifier.trim();
        String password = passwordChars == null ? "" : new String(passwordChars);

        try {
            if (cleanIdentifier.isEmpty() || password.isEmpty()) {
                return LoginResult.failure(
                        AuthStatus.EMPTY_FIELDS,
                        "Please enter your email/username and password."
                );
            }

            Optional<User> optionalUser = userRepository.findByEmailOrUsername(cleanIdentifier);
            if (optionalUser.isEmpty()) {
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

            // QueueTees requirement: customers must verify their email before customer access.
            if (user.getRole() == UserRole.CUSTOMER && !user.isEmailVerified()) {
                return LoginResult.failure(
                        AuthStatus.EMAIL_NOT_VERIFIED,
                        "Please verify your email before logging in.",
                        user
                );
            }

            // QueueTees requirement: staff must be approved by an administrator first.
            if (user.getRole() == UserRole.STAFF && user.getStatus() != AccountStatus.ACTIVE) {
                return LoginResult.failure(
                        AuthStatus.STAFF_NOT_APPROVED,
                        "Your staff account is still waiting for administrator approval."
                );
            }

            if (user.getStatus() != AccountStatus.ACTIVE) {
                return LoginResult.failure(
                        AuthStatus.INVALID_CREDENTIALS,
                        "This account cannot log in right now."
                );
            }

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
                "Invalid email/username or password."
        );
    }

    public void verifyEmail(User user) {
        user.setEmailVerified(true);
    }

    public RegistrationResult registerCustomer(
            String email,
            String fullname,
            String username,
            char[] passwordChars,
            String address,
            String contactnum,
            String gender,
            LocalDate birthday) {

        String cleanEmail    = email    == null ? "" : email.trim();
        String cleanUsername = username == null ? "" : username.trim();
        String cleanFullname = fullname == null ? "" : fullname.trim();
        String password      = passwordChars == null ? "" : new String(passwordChars);

        try {
            // Empty fields check
            if (cleanEmail.isEmpty() || cleanUsername.isEmpty()
                    || cleanFullname.isEmpty() || password.isEmpty()) {
                return RegistrationResult.failure(
                        AuthStatus.EMPTY_FIELDS,
                        "Please fill in all required fields."
                );
            }

            // Basic email format check
            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                return RegistrationResult.failure(
                        AuthStatus.INVALID_EMAIL,
                        "Please enter a valid email address."
                );
            }

            // Duplicate email check
            if (userRepository.findByEmailOrUsername(cleanEmail).isPresent()) {
                return RegistrationResult.failure(
                        AuthStatus.DUPLICATE_EMAIL,
                        "That email is already registered."
                );
            }

            // Duplicate username check
            if (userRepository.findByEmailOrUsername(cleanUsername).isPresent()) {
                return RegistrationResult.failure(
                        AuthStatus.DUPLICATE_USERNAME,
                        "That username is already taken."
                );
            }

            // Gender check
            if (gender == null || gender.isEmpty()) {
                return RegistrationResult.failure(
                        AuthStatus.EMPTY_FIELDS,
                        "Please select a gender."
                );
            }

            // Birthday check
            if (birthday == null) {
                return RegistrationResult.failure(
                        AuthStatus.EMPTY_FIELDS,
                        "Please enter your birthday."
                );
            }

            // Age check — must be at least 18
            int age = Period.between(birthday, LocalDate.now()).getYears();
            if (age < 18) {
                return RegistrationResult.failure(
                        AuthStatus.EMPTY_FIELDS,
                        "You must be at least 18 years old to register."
                );
            }

            // Create and save user
            int newId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            User newUser = new User(
                    newId,
                    cleanUsername,
                    cleanEmail,
                    PasswordUtil.hashPassword(password),
                    UserRole.CUSTOMER,
                    false,
                    AccountStatus.ACTIVE
            );

            userRepository.save(newUser);

            return RegistrationResult.success(newUser);

        } finally {
            if (passwordChars != null) {
                java.util.Arrays.fill(passwordChars, '\0');
            }
        }
    }
}
