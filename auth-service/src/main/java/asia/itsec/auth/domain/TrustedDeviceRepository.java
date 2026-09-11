package asia.itsec.auth.domain;

public interface TrustedDeviceRepository {
    String issue(String userId);
    String resolve(String rememberToken);
}
