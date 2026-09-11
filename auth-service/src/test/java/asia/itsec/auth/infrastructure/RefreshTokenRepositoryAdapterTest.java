package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenRepositoryAdapterTest {

    @Mock private JpaRefreshTokenRepository jpaRepository;

    private RefreshTokenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RefreshTokenRepositoryAdapter(jpaRepository);
    }

    private RefreshTokenEntity entity() {
        return RefreshTokenEntity.builder()
                .id("rt-1").userId("user-1").tokenHash("hash").expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false).build();
    }

    @Test
    void save_mapsDomainToEntityAndBack() {
        RefreshToken token = RefreshToken.builder()
                .id("rt-1").userId("user-1").tokenHash("hash").expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false).build();
        when(jpaRepository.save(any(RefreshTokenEntity.class))).thenReturn(entity());

        RefreshToken result = adapter.save(token);

        assertThat(result.getId()).isEqualTo("rt-1");
    }

    @Test
    void findByTokenHash_found_mapsToDomain() {
        when(jpaRepository.findByTokenHash("hash")).thenReturn(Optional.of(entity()));

        Optional<RefreshToken> result = adapter.findByTokenHash("hash");

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("user-1");
    }

    @Test
    void findByTokenHash_notFound_returnsEmpty() {
        when(jpaRepository.findByTokenHash("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findByTokenHash("missing")).isEmpty();
    }

    @Test
    void revoke_whenFound_setsRevokedAndSaves() {
        RefreshTokenEntity found = entity();
        when(jpaRepository.findById("rt-1")).thenReturn(Optional.of(found));

        adapter.revoke("rt-1");

        assertThat(found.isRevoked()).isTrue();
        verify(jpaRepository).save(found);
    }

    @Test
    void revoke_whenNotFound_doesNothing() {
        when(jpaRepository.findById("ghost")).thenReturn(Optional.empty());

        adapter.revoke("ghost");

        verify(jpaRepository, never()).save(any());
    }
}
