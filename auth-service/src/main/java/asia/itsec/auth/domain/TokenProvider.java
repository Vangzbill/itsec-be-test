package asia.itsec.auth.domain;

public interface TokenProvider {
    String generateAccessToken(User user);
    String generateRefreshToken();
    boolean validateToken(String token);
    long getRemainingValiditySeconds(String token);
}
