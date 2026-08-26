package repository;

import java.util.Optional;
import model.User;

public interface UserRepository {
    Optional<User> findByEmailOrUsername(String identifier);
    void save(User user);
}
