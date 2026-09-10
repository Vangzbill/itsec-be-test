package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.Role;
import asia.itsec.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String fullname;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String passwordHash;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles;

    public User toDomain() {
        return User.builder()
                .id(id)
                .fullname(fullname)
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .roles(roles.stream().map(r -> Role.valueOf(r.getName())).collect(Collectors.toSet()))
                .build();
    }
}
