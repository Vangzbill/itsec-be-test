package asia.itsec.auth.application;

import asia.itsec.auth.domain.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginAttemptRepository loginAttemptRepository;
    @Mock private OtpSender otpSender;
    @Mock private OtpRepository otpRepository;
    @Mock private TokenProvider tokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenDenylistRepository tokenDenylistRepository;
    @Mock private TrustedDeviceRepository trustedDeviceRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private HttpServletRequest httpServletRequest;

    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, loginAttemptRepository, otpSender,
                otpRepository, tokenProvider, refreshTokenRepository, tokenDenylistRepository,
                trustedDeviceRepository, eventPublisher, httpServletRequest);

        user = User.builder()
                .id("user-1")
                .fullname("Test User")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .roles(Set.of(Role.VIEWER))
                .build();

        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(httpServletRequest.getRequestURI()).thenReturn("/auth/login");
        when(httpServletRequest.getMethod()).thenReturn("POST");
    }

    @Test
    void register_success_savesUserWithViewerRole() {
        RegisterRequest request = new RegisterRequest();
        request.setFullname("New User");
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("Passw0rd!");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register(request);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getRoles()).containsExactly(Role.VIEWER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("taken");
        request.setEmail("a@b.com");
        request.setFullname("A");
        request.setPassword("Passw0rd!");

        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("free");
        request.setEmail("taken@b.com");
        request.setFullname("A");
        request.setPassword("Passw0rd!");

        when(userRepository.existsByUsername("free")).thenReturn(false);
        when(userRepository.existsByEmail("taken@b.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void login_success_sendsOtpAndReturnsTemporaryToken() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("correct");

        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.isLocked("user-1")).thenReturn(false);
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        LoginResponse response = authService.login(request);

        assertThat(response.getTemporaryToken()).isNotBlank();
        assertThat(response.getAccessToken()).isNull();
        verify(loginAttemptRepository).resetAttempts("user-1");
        verify(otpRepository).save(anyString(), eq("hashed-otp"), eq("user-1"));
        verify(otpSender).sendOtp(eq("test@example.com"), anyString());
    }

    @Test
    void login_wrongPassword_recordsFailedAttemptAndThrows() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("wrong");

        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.isLocked("user-1")).thenReturn(false);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Invalid credentials");
        verify(loginAttemptRepository).recordFailedAttempt("user-1");
        verify(otpSender, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void login_accountLocked_throwsAccountLockedException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("whatever");

        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.isLocked("user-1")).thenReturn(true);
        when(loginAttemptRepository.getLockoutRemainingSeconds("user-1")).thenReturn(900L);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_withValidRememberToken_skipsOtpAndReturnsTokensDirectly() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("correct");
        request.setRememberToken("device-token");

        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.isLocked("user-1")).thenReturn(false);
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(trustedDeviceRepository.resolve("device-token")).thenReturn("user-1");
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getTemporaryToken()).isNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(otpSender, never()).sendOtp(anyString(), anyString());
        verify(eventPublisher).publish(any());
    }

    @Test
    void login_withInvalidRememberToken_fallsBackToOtp() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("correct");
        request.setRememberToken("stale-token");

        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.isLocked("user-1")).thenReturn(false);
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(trustedDeviceRepository.resolve("stale-token")).thenReturn(null);

        LoginResponse response = authService.login(request);

        assertThat(response.getTemporaryToken()).isNotBlank();
        verify(otpSender).sendOtp(anyString(), anyString());
    }

    @Test
    void verifyOtp_success_returnsTokensAndRememberToken() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setTemporaryToken("temp-token");
        request.setCode("123456");

        when(otpRepository.verify("temp-token", "123456")).thenReturn("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(trustedDeviceRepository.issue("user-1")).thenReturn("remember-token");

        TokenResponse response = authService.verifyOtp(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRememberToken()).isEqualTo("remember-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void verifyOtp_userNotFound_throws() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setTemporaryToken("temp-token");
        request.setCode("123456");

        when(otpRepository.verify("temp-token", "123456")).thenReturn("ghost-id");
        when(userRepository.findById("ghost-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("User not found");
    }

    @Test
    void refreshToken_success_returnsNewAccessToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("raw-refresh-token");

        RefreshToken stored = RefreshToken.builder()
                .id("rt-1")
                .userId("user-1")
                .tokenHash("whatever")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("new-access-token");

        TokenResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("raw-refresh-token");
    }

    @Test
    void refreshToken_notFound_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("unknown");

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void refreshToken_expiredOrRevoked_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("raw-refresh-token");

        RefreshToken revoked = RefreshToken.builder()
                .id("rt-1")
                .userId("user-1")
                .tokenHash("whatever")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Refresh token is expired or revoked");
    }

    @Test
    void logout_denylistsAccessTokenAndRevokesRefreshToken() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("raw-refresh-token");

        RefreshToken stored = RefreshToken.builder()
                .id("rt-1")
                .userId("user-1")
                .tokenHash("whatever")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(tokenProvider.getRemainingValiditySeconds("access-token")).thenReturn(120L);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout("access-token", request);

        verify(tokenDenylistRepository).denylist("access-token", 120L);
        verify(refreshTokenRepository).revoke("rt-1");
    }

    @Test
    void listUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = authService.listUsers();

        assertThat(result).containsExactly(user);
    }

    @Test
    void updateUser_success_updatesFullnameAndEmail() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullname("Updated Name");
        request.setEmail("updated@example.com");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.updateUser("user-1", request);

        assertThat(result.getFullname()).isEqualTo("Updated Name");
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        verify(eventPublisher).publish(any());
    }

    @Test
    void updateUser_notFound_throws() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullname("X");
        request.setEmail("x@x.com");

        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUser("ghost", request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteUser_success_deletesAndPublishesEvent() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        authService.deleteUser("user-1");

        verify(userRepository).deleteById("user-1");
        verify(eventPublisher).publish(any());
    }
}
