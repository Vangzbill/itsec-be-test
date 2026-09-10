package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    public RefreshToken toDomain() {
        return RefreshToken.builder()
                .id(id)
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();
    }

    public static RefreshTokenEntity fromDomain(RefreshToken refreshToken) {
        return RefreshTokenEntity.builder()
                .id(refreshToken.getId())
                .userId(refreshToken.getUserId())
                .tokenHash(refreshToken.getTokenHash())
                .expiresAt(refreshToken.getExpiresAt())
                .revoked(refreshToken.isRevoked())
                .build();
    }
}
