package asia.itsec.auth.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class User {
    private String id;
    private String fullname;
    private String username;
    private String email;
    private String passwordHash;
    private Set<Role> roles;
}
