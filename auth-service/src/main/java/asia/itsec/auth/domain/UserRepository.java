package asia.itsec.auth.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(String id);
    Optional<User> findByUsernameOrEmail(String identifier);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findAll();
    void deleteById(String id);
}
