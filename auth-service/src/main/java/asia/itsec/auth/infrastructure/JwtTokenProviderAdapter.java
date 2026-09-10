package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.TokenProvider;
import asia.itsec.auth.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProviderAdapter implements TokenProvider {

    // Ideally injected from properties, but hardcoded here for simplicity
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long ACCESS_TOKEN_VALIDITY_MS = 15 * 60 * 1000; // 15 minutes

    @Override
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY_MS);

        return Jwts.builder()
                .setSubject(user.getId())
                .claim("roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toList()))
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
