package asia.itsec.auth.domain;

public interface LoginAttemptRepository {
    void recordFailedAttempt(String userId);
    void resetAttempts(String userId);
    boolean isLocked(String userId);
    long getLockoutRemainingSeconds(String userId);
}
