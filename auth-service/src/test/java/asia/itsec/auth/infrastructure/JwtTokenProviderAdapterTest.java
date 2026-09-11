package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.Role;
import asia.itsec.auth.domain.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderAdapterTest {

    private final JwtTokenProviderAdapter provider = new JwtTokenProviderAdapter();

    private User user() {
        return User.builder()
                .id("user-1").fullname("Test").username("testuser").email("test@example.com")
                .passwordHash("hashed").roles(Set.of(Role.EDITOR)).build();
    }

    @Test
    void generateAccessToken_producesValidToken() {
        String token = provider.generateAccessToken(user());

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void generateRefreshToken_producesNonEmptyRandomString() {
        String token1 = provider.generateRefreshToken();
        String token2 = provider.generateRefreshToken();

        assertThat(token1).isNotBlank();
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void validateToken_garbageString_returnsFalse() {
        assertThat(provider.validateToken("not-a-real-jwt")).isFalse();
    }

    @Test
    void getRemainingValiditySeconds_freshToken_isPositiveAndBoundedByFifteenMinutes() {
        String token = provider.generateAccessToken(user());

        long remaining = provider.getRemainingValiditySeconds(token);

        assertThat(remaining).isGreaterThan(0).isLessThanOrEqualTo(15 * 60);
    }

    @Test
    void getRemainingValiditySeconds_invalidToken_returnsZero() {
        assertThat(provider.getRemainingValiditySeconds("garbage")).isZero();
    }
}
