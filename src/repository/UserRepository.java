package repository;

import java.util.Optional;
import model.User;

public interface UserRepository {
    Optional<User> findByEmailOrUsername(String identifier);
    void save(User user);
    java.util.List<User> findAll();
    default int nextId() { return findAll().stream().mapToInt(User::getId).max().orElse(0) + 1; }
    default void saveWithActivity(User user, User actor, String action, String details) {
        save(user); service.ActivityLogger.record(actor,action,details);
    }
}
