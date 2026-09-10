package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.TokenProvider;
import asia.itsec.auth.domain.User;
import asia.itsec.shared.security.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProviderAdapter implements TokenProvider {

    private final Key key = JwtUtils.getKey();
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

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getRemainingValiditySeconds(String token) {
        try {
            Date expiration = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration();
            long diff = expiration.getTime() - System.currentTimeMillis();
            return diff > 0 ? diff / 1000 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
