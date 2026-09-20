package asia.itsec.shared.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(redis);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String token() {
        return Jwts.builder()
                .setSubject("user-1")
                .claim("roles", List.of("VIEWER"))
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(JwtUtils.getKey())
                .compact();
    }

    private void run(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void validToken_setsAuthentication() throws Exception {
        run(token());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user-1");
    }

    @Test
    void denylistedToken_staysAnonymous() throws Exception {
        String token = token();
        when(redis.hasKey(JwtAuthenticationFilter.DENYLIST_PREFIX + token)).thenReturn(true);

        run(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
