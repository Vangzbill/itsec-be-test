package asia.itsec.shared.security;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;

public class JwtUtils {
    private static final int MIN_SECRET_BYTES = 32; // HS256 needs >= 256 bits

    public static Key getKey() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET env var must be set (min " + MIN_SECRET_BYTES + " bytes)");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
