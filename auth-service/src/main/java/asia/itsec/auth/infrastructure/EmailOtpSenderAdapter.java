package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.OtpSender;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailOtpSenderAdapter implements OtpSender {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtp(String destination, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@itsec.asia");
        message.setTo(destination);
        message.setSubject("Your Login OTP");
        message.setText("Your OTP is: " + otpCode + "\n\nIt expires in 5 minutes.");
        mailSender.send(message);
    }
}
