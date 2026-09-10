package asia.itsec.shared.security;

import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;

public class JwtUtils {
    // secret key for local development
    private static final String SECRET = "aW5zZWN1cmVfc2VjcmV0X2tleV9mb3JfaXRzZWNfYXNzaWdubWVudF8xMjM0NTY3ODk=";
    
    public static Key getKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
    }
}
