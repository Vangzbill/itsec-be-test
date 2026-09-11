package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.Role;
import asia.itsec.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserRepositoryAdapterTest {

    @Mock private JpaUserRepository jpaUserRepository;
    @Mock private RoleRepository roleRepository;

    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserRepositoryAdapter(jpaUserRepository, roleRepository);
    }

    private UserEntity entity(String id, Set<RoleEntity> roles) {
        return UserEntity.builder()
                .id(id)
                .fullname("Test")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .roles(roles)
                .build();
    }

    @Test
    void save_reusesExistingRole_whenAlreadyPresent() {
        User user = User.builder()
                .id("user-1").fullname("Test").username("testuser").email("test@example.com")
                .passwordHash("hashed").roles(Set.of(Role.VIEWER)).build();

        RoleEntity existingRole = new RoleEntity("VIEWER");
        when(roleRepository.findByName("VIEWER")).thenReturn(Optional.of(existingRole));
        when(jpaUserRepository.save(any(UserEntity.class)))
                .thenReturn(entity("user-1", Set.of(existingRole)));

        User result = adapter.save(user);

        assertThat(result.getId()).isEqualTo("user-1");
        verify(roleRepository, never()).save(any());
    }

    @Test
    void save_createsRole_whenNotYetPresent() {
        User user = User.builder()
                .id("user-1").fullname("Test").username("testuser").email("test@example.com")
                .passwordHash("hashed").roles(Set.of(Role.EDITOR)).build();

        RoleEntity newRole = new RoleEntity("EDITOR");
        when(roleRepository.findByName("EDITOR")).thenReturn(Optional.empty());
        when(roleRepository.save(any(RoleEntity.class))).thenReturn(newRole);
        when(jpaUserRepository.save(any(UserEntity.class))).thenReturn(entity("user-1", Set.of(newRole)));

        adapter.save(user);

        verify(roleRepository).save(any(RoleEntity.class));
    }

    @Test
    void findById_found_mapsToDomain() {
        when(jpaUserRepository.findById("user-1"))
                .thenReturn(Optional.of(entity("user-1", Set.of(new RoleEntity("VIEWER")))));

        Optional<User> result = adapter.findById("user-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("user-1");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(jpaUserRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThat(adapter.findById("ghost")).isEmpty();
    }

    @Test
    void findByUsernameOrEmail_delegatesWithSameIdentifierTwice() {
        when(jpaUserRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(entity("user-1", Set.of(new RoleEntity("VIEWER")))));

        Optional<User> result = adapter.findByUsernameOrEmail("testuser");

        assertThat(result).isPresent();
    }

    @Test
    void existsByUsername_delegatesToJpa() {
        when(jpaUserRepository.existsByUsername("taken")).thenReturn(true);
        assertThat(adapter.existsByUsername("taken")).isTrue();
    }

    @Test
    void existsByEmail_delegatesToJpa() {
        when(jpaUserRepository.existsByEmail("taken@x.com")).thenReturn(true);
        assertThat(adapter.existsByEmail("taken@x.com")).isTrue();
    }

    @Test
    void findAll_mapsAllEntitiesToDomain() {
        when(jpaUserRepository.findAll())
                .thenReturn(List.of(entity("user-1", Set.of(new RoleEntity("VIEWER")))));

        List<User> result = adapter.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void deleteById_delegatesToJpa() {
        adapter.deleteById("user-1");
        verify(jpaUserRepository).deleteById("user-1");
    }
}
