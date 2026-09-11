package asia.itsec.auth.application;

import asia.itsec.auth.domain.*;
import asia.itsec.shared.event.AuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final TokenDenylistRepository tokenDenylistRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final EventPublisher eventPublisher;
    private final HttpServletRequest httpServletRequest;

    private final SecureRandom secureRandom = new SecureRandom();

    private AuditEvent buildAuditEvent(String action, String actorId, String actorUsername, String status) {
        return AuditEvent.builder()
                .actorId(actorId)
                .actorUsername(actorUsername)
                .action(action)
                .entityType("USER")
                .entityId(actorId)
                .ipAddress(httpServletRequest.getRemoteAddr())
                .userAgent(httpServletRequest.getHeader("User-Agent"))
                .requestPath(httpServletRequest.getRequestURI())
                .httpMethod(httpServletRequest.getMethod())
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
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

        String rememberToken = request.getRememberToken();
        if (rememberToken != null && !rememberToken.isBlank()
                && user.getId().equals(trustedDeviceRepository.resolve(rememberToken))) {
            TokenResponse tokens = issueTokens(user);
            eventPublisher.publish(buildAuditEvent("LOGIN_SUCCESS", user.getId(), user.getUsername(), "SUCCESS"));
            return new LoginResponse(null, tokens.getAccessToken(), tokens.getRefreshToken());
        }

        String tempToken = UUID.randomUUID().toString();
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));

        otpRepository.save(tempToken, passwordEncoder.encode(otpCode), user.getId());
        otpSender.sendOtp(user.getEmail(), otpCode);

        return new LoginResponse(tempToken, null, null);
    }

    public TokenResponse verifyOtp(VerifyOtpRequest request) {
        String userId = otpRepository.verify(request.getTemporaryToken(), request.getCode());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found"));

        TokenResponse tokens = issueTokens(user);
        String rememberToken = trustedDeviceRepository.issue(user.getId());
        eventPublisher.publish(buildAuditEvent("LOGIN_SUCCESS", user.getId(), user.getUsername(), "SUCCESS"));
        return new TokenResponse(tokens.getAccessToken(), tokens.getRefreshToken(), rememberToken);
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public User updateUser(String id, UpdateUserRequest request) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<Role> roles = request.getRoles() != null && !request.getRoles().isEmpty()
                ? request.getRoles().stream().map(Role::valueOf).collect(Collectors.toSet())
                : existing.getRoles();

        User updated = User.builder()
                .id(existing.getId())
                .fullname(request.getFullname())
                .username(existing.getUsername())
                .email(request.getEmail())
                .passwordHash(existing.getPasswordHash())
                .roles(roles)
                .build();

        User saved = userRepository.save(updated);
        eventPublisher.publish(buildAuditEvent("USER_UPDATED", id, saved.getUsername(), "SUCCESS"));
        return saved;
    }

    public void deleteUser(String id) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.deleteById(id);
        eventPublisher.publish(buildAuditEvent("USER_DELETED", id, existing.getUsername(), "SUCCESS"));
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String hashedToken = hashToken(request.getRefreshToken());
        
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new SecurityException("Invalid refresh token"));
                
        if (!refreshToken.isValid()) {
            throw new SecurityException("Refresh token is expired or revoked");
        }
        
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new SecurityException("User not found"));
                
        String newAccessToken = tokenProvider.generateAccessToken(user);

        return new TokenResponse(newAccessToken, request.getRefreshToken(), null);
    }
    
    public void logout(String accessToken, LogoutRequest request) {
        long remainingSeconds = tokenProvider.getRemainingValiditySeconds(accessToken);
        if (remainingSeconds > 0) {
            tokenDenylistRepository.denylist(accessToken, remainingSeconds);
        }

        String hashedToken = hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(hashedToken)
                .ifPresent(rt -> refreshTokenRepository.revoke(rt.getId()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getDetails() != null ? auth.getDetails().toString() : null;
            eventPublisher.publish(buildAuditEvent("LOGOUT", auth.getName(), username, "SUCCESS"));
        }
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

        return new TokenResponse(accessToken, rawRefreshToken, null);
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
