package asia.itsec.auth.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRequest {
    @NotBlank
    private String fullname;

    @NotBlank
    @Email
    private String email;

    private Set<String> roles;
}
