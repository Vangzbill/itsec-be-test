package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.RefreshToken;
import asia.itsec.auth.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpaRepository;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = RefreshTokenEntity.fromDomain(refreshToken);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(RefreshTokenEntity::toDomain);
    }

    @Override
    public void revoke(String tokenId) {
        jpaRepository.findById(tokenId).ifPresent(entity -> {
            entity.setRevoked(true);
            jpaRepository.save(entity);
        });
    }
}
