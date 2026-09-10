package asia.itsec.auth.application;

import asia.itsec.auth.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
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
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    
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
        
        User user = userRepository.findByUsernameOrEmail(userId)
                .orElseThrow(() -> new SecurityException("User not found"));
                
        return issueTokens(user);
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String hashedToken = hashToken(request.getRefreshToken());
        
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new SecurityException("Invalid refresh token"));
                
        if (!refreshToken.isValid()) {
            throw new SecurityException("Refresh token is expired or revoked");
        }
        
        User user = userRepository.findByUsernameOrEmail(refreshToken.getUserId())
                .orElseThrow(() -> new SecurityException("User not found"));
                
        String newAccessToken = tokenProvider.generateAccessToken(user);
        
        return new TokenResponse(newAccessToken, request.getRefreshToken());
    }
    
    private TokenResponse issueTokens(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();
                
        refreshTokenRepository.save(refreshToken);
        
        return new TokenResponse(accessToken, rawRefreshToken);
    }
    
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
