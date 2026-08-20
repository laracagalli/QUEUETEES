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
    public void save(User user) {
        users.add(user);
    }
}
