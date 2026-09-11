package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.User;
import asia.itsec.auth.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final RoleRepository roleRepository;

    @Override
    public User save(User user) {
        Set<RoleEntity> roleEntities = user.getRoles().stream()
                .map(role -> roleRepository.findByName(role.name())
                        .orElseGet(() -> roleRepository.save(new RoleEntity(role.name()))))
                .collect(Collectors.toSet());

        UserEntity entity = UserEntity.builder()
                .id(user.getId())
                .fullname(user.getFullname())
                .username(user.getUsername())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .roles(roleEntities)
                .build();

        return jpaUserRepository.save(entity).toDomain();
    }

    @Override
    public Optional<User> findById(String id) {
        return jpaUserRepository.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String identifier) {
        return jpaUserRepository.findByUsernameOrEmail(identifier, identifier)
                .map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }
}
