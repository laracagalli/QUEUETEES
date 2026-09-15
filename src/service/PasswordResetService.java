package service;

import java.time.Clock;
import java.util.*;
import model.User;
import repository.UserRepository;

/** Reset challenges belong to one application session; no codes are exposed to the UI. */
public final class PasswordResetService {
    @FunctionalInterface public interface Sender { void send(String email, String code) throws Exception; }
    private final UserRepository users;
    private final Sender sender;
    private final Clock clock;
    private final Map<String, Challenge> challenges = new HashMap<>();
    private final Map<String, Long> lastSent = new HashMap<>();
    private static final long LIFETIME = 10 * 60_000;
    private static final class Challenge {
        User user; String code; long expires; int attempts; boolean verified;
    }
    public PasswordResetService(UserRepository users, Sender sender) { this(users, sender, Clock.systemUTC()); }
    public PasswordResetService(UserRepository users, Sender sender, Clock clock) {
        this.users = users; this.sender = sender; this.clock = clock;
    }
    public synchronized String request(String email) throws Exception {
        String clean = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!clean.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Enter a valid email address.");
        long now = clock.millis();
        challenges.values().removeIf(c -> c.expires <= now);
        lastSent.entrySet().removeIf(e -> now - e.getValue() >= 60_000);
        if (lastSent.containsKey(clean)) throw new IllegalArgumentException("Please wait one minute before requesting another code.");

        Challenge c = new Challenge();
        c.user = users.findByEmailOrUsername(clean).filter(u -> u.getEmail().equalsIgnoreCase(clean)).orElse(null);
        c.code = backend.OtpUtil.generateOTP(); c.expires = now + LIFETIME;
        if (c.user != null) sender.send(c.user.getEmail(), c.code);
        c.expires = clock.millis() + LIFETIME;
        lastSent.put(clean, clock.millis());
        challenges.values().removeIf(old -> old.user != null && old.user == c.user);
        String token = UUID.randomUUID().toString();
        challenges.put(token, c);
        return token;
    }
    private Challenge current(String token) {
        Challenge c = challenges.get(token);
        if (c == null || clock.millis() >= c.expires) {
            challenges.remove(token);
            throw new IllegalArgumentException("Code expired. Request a new code.");
        }
        return c;
    }
    public synchronized void verify(String token, String code) {
        Challenge c = current(token);
        if (code == null || !code.matches("[0-9]{6}"))
            throw new IllegalArgumentException("Please enter all six digits.");
        if (++c.attempts > 5) { challenges.remove(token); throw new IllegalArgumentException("Too many attempts. Request a new code."); }
        if (c.user == null || !c.code.equals(code)) throw new IllegalArgumentException("Incorrect code. Check your email and try again.");
        c.verified = true;
    }
    public synchronized void reset(String token, char[] password, char[] confirmation) {
        try {
            Challenge c = current(token);
            if (!c.verified) throw new IllegalArgumentException("Verify your email code first.");
            if (!Arrays.equals(password, confirmation)) throw new IllegalArgumentException("Passwords do not match.");
            String value = new String(password);
            String error = PasswordUtil.getPasswordValidationMessage(value);
            if (error != null) throw new IllegalArgumentException(error);
            c.user.setPasswordHash(PasswordUtil.hashPassword(value));
            challenges.values().removeIf(other -> other.user == c.user);
        } finally { Arrays.fill(password, '\0'); Arrays.fill(confirmation, '\0'); }
    }
}
