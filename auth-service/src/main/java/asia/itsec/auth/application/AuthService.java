package asia.itsec.auth.application;

import asia.itsec.auth.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptRepository loginAttemptRepository;
    private final OtpSender otpSender;
    private final OtpRepository otpRepository;
    
    private final SecureRandom secureRandom = new SecureRandom();

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .id(request.getId())
                .fullname(request.getFullname())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(Role.VIEWER))
                .build();

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        String genericFailureMessage = "Invalid credentials";

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new SecurityException(genericFailureMessage));

        if (loginAttemptRepository.isLocked(user.getId())) {
            throw new AccountLockedException(loginAttemptRepository.getLockoutRemainingSeconds(user.getId()));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptRepository.recordFailedAttempt(user.getId());
            throw new SecurityException(genericFailureMessage);
        }

        loginAttemptRepository.resetAttempts(user.getId());

        String tempToken = UUID.randomUUID().toString();
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        
        otpRepository.save(tempToken, passwordEncoder.encode(otpCode), user.getId());
        otpSender.sendOtp(user.getEmail(), otpCode);

        return new LoginResponse(tempToken);
    }

    public TokenResponse verifyOtp(VerifyOtpRequest request) {
        String userId = otpRepository.verify(request.getTemporaryToken(), request.getCode());
        
        // Return dummy tokens until JWT is implemented in Phase 3d
        return new TokenResponse("dummy-access-token-for-" + userId, "dummy-refresh-token");
    }
}
