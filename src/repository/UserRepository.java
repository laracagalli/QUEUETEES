package repository;

import java.util.Optional;
import model.User;

public interface UserRepository {
    Optional<User> findByEmailOrUsername(String identifier);
    boolean emailExists(String email);
    boolean usernameExists(String username);
    int nextId();
    void save(User user);
}
