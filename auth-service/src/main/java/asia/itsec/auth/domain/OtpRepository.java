package asia.itsec.auth.domain;

public interface OtpRepository {
    void save(String tempToken, String hashedCode, String userId);
    String verify(String tempToken, String rawCode);
}
