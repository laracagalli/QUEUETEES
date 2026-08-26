package repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.User;

public class InMemoryUserRepository implements UserRepository {
    private final List<User> users = new ArrayList<>();

    @Override
    public Optional<User> findByEmailOrUsername(String identifier) {
        if (identifier == null) {
            return Optional.empty();
        }

        String normalized = identifier.trim();
        return users.stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(normalized)
                        || user.getUsername().equalsIgnoreCase(normalized))
                .findFirst();
    }

    @Override
    public boolean emailExists(String email) {
        if (email == null) return false;
        return users.stream().anyMatch(user -> user.getEmail().equalsIgnoreCase(email.trim()));
    }

    @Override
    public boolean usernameExists(String username) {
        if (username == null) return false;
        return users.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username.trim()));
    }

    @Override
    public int nextId() {
        return users.stream().mapToInt(User::getId).max().orElse(0) + 1;
    }

    @Override
    public void save(User user) {
        users.add(user);
    }
}
