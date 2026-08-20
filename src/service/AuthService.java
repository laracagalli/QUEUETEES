package service;

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
                        "Please verify your email before logging in."
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
}
