package asia.itsec.auth.domain;

public interface TokenDenylistRepository {
    void denylist(String token, long remainingTtlSeconds);
    boolean isDenylisted(String token);
}
