package asia.itsec.auth.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String temporaryToken;

    @NotBlank
    private String code;
}
