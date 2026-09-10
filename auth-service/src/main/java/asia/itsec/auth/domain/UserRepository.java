package asia.itsec.auth.domain;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByUsernameOrEmail(String identifier);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
