package asia.itsec.auth.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RefreshToken {
    private String id;
    private String userId;
    private String tokenHash;
    private Instant expiresAt;
    private boolean revoked;
    
    public boolean isValid() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }
}
