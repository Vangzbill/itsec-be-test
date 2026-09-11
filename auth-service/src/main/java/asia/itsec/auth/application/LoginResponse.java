package asia.itsec.auth.application;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String temporaryToken;
    private String accessToken;
    private String refreshToken;
}
