package service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import model.AccountStatus;
import model.User;
import model.UserRole;
import repository.UserRepository;

public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,20}$");
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿÑñ .'-]{2,60}$");
    private static final Pattern CONTACT_PATTERN = Pattern.compile("^09\\d{9}$");

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

            if (cleanIdentifier.length() > 100 || password.length() > 128) {
                return LoginResult.failure(AuthStatus.INVALID_CREDENTIALS, "Invalid credentials.");
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

            if (user.getRole() == UserRole.CUSTOMER && !user.isEmailVerified()) {
                return LoginResult.emailVerificationRequired(user);
            }

            if (user.getRole() == UserRole.STAFF && user.getStatus() != AccountStatus.ACTIVE) {
                return LoginResult.failure(
                        AuthStatus.STAFF_NOT_APPROVED,
                        "Your staff account is still waiting for administrator approval."
                );
            }

            if (user.getStatus() != AccountStatus.ACTIVE) {
                return LoginResult.failure(AuthStatus.INVALID_CREDENTIALS, "This account cannot log in right now.");
            }

            return LoginResult.success(user);
        } finally {
            if (passwordChars != null) {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    public RegistrationResult registerCustomer(
            String email,
            String fullName,
            String username,
            char[] passwordChars,
            String address,
            String contactNumber,
            String gender,
            LocalDate birthday) {

        String cleanEmail = clean(email);
        String cleanFullName = clean(fullName);
        String cleanUsername = clean(username);
        String cleanAddress = clean(address);
        String cleanContact = clean(contactNumber);
        String cleanGender = clean(gender);
        String password = passwordChars == null ? "" : new String(passwordChars);

        try {
            if (cleanEmail.isEmpty() || cleanFullName.isEmpty() || cleanUsername.isEmpty()
                    || password.isEmpty() || cleanAddress.isEmpty() || cleanContact.isEmpty()
                    || cleanGender.isEmpty() || birthday == null) {
                return RegistrationResult.failure("Please complete all registration fields.");
            }

            if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
                return RegistrationResult.failure("Please enter a valid email address.");
            }

            if (!FULL_NAME_PATTERN.matcher(cleanFullName).matches()) {
                return RegistrationResult.failure("Full name may only contain letters, spaces, apostrophes, periods, and hyphens.");
            }

            if (!USERNAME_PATTERN.matcher(cleanUsername).matches()) {
                return RegistrationResult.failure("Username must be 4-20 characters using letters, numbers, or underscore only.");
            }

            String passwordError = validatePassword(password);
            if (passwordError != null) {
                return RegistrationResult.failure(passwordError);
            }

            if (cleanAddress.length() < 5 || cleanAddress.length() > 150) {
                return RegistrationResult.failure("Please enter a valid full address.");
            }

            if (!CONTACT_PATTERN.matcher(cleanContact).matches()) {
                return RegistrationResult.failure("Contact number must be 11 digits and start with 09.");
            }

            if (birthday.isAfter(LocalDate.now())) {
                return RegistrationResult.failure("Birthday cannot be in the future.");
            }

            int age = Period.between(birthday, LocalDate.now()).getYears();
            if (age < 18) {
                return RegistrationResult.failure("Only customers who are 18 years old or older can sign up.");
            }
            if (age > 120) {
                return RegistrationResult.failure("Please enter a valid birthday.");
            }

            if (userRepository.emailExists(cleanEmail)) {
                return RegistrationResult.failure("That email address is already registered.");
            }

            if (userRepository.usernameExists(cleanUsername)) {
                return RegistrationResult.failure("That username is already taken.");
            }

            User user = new User(
                    userRepository.nextId(),
                    cleanUsername,
                    cleanEmail,
                    PasswordUtil.hashPassword(password),
                    UserRole.CUSTOMER,
                    false,
                    AccountStatus.ACTIVE,
                    cleanFullName,
                    cleanAddress,
                    cleanContact,
                    cleanGender,
                    birthday
            );

            userRepository.save(user);
            return RegistrationResult.success(user);
        } finally {
            if (passwordChars != null) {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    public void verifyEmail(User user) {
        if (user != null && user.getRole() == UserRole.CUSTOMER) {
            user.setEmailVerified(true);
        }
    }

    private String validatePassword(String password) {
        if (password.length() < 8 || password.length() > 64) {
            return "Password must be 8-64 characters long.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number.";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "Password must contain at least one special character.";
        }
        return null;
    }

    private LoginResult invalidCredentials() {
        return LoginResult.failure(AuthStatus.INVALID_CREDENTIALS, "Invalid email/username or password.");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
