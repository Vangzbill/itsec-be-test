package asia.itsec.auth.application;

import lombok.Getter;

@Getter
public class AccountLockedException extends RuntimeException {
    private final long remainingSeconds;

    public AccountLockedException(long remainingSeconds) {
        super(String.format("Account is locked. Try again in %d seconds.", remainingSeconds));
        this.remainingSeconds = remainingSeconds;
    }
}
