package asia.itsec.auth.domain;

public interface OtpSender {
    void sendOtp(String destination, String otpCode);
}
