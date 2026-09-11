package asia.itsec.auth.api;

import asia.itsec.auth.application.*;
import asia.itsec.auth.domain.Role;
import asia.itsec.auth.domain.TokenDenylistRepository;
import asia.itsec.auth.domain.TokenProvider;
import asia.itsec.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private TokenProvider tokenProvider;
    @Mock private TokenDenylistRepository tokenDenylistRepository;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, tokenProvider, tokenDenylistRepository);
    }

    private User user() {
        return User.builder().id("user-1").fullname("Test").username("testuser")
                .email("test@example.com").passwordHash("hashed").roles(Set.of(Role.VIEWER)).build();
    }

    @Test
    void register_returnsCreatedWithUser() {
        RegisterRequest request = new RegisterRequest();
        when(authService.register(request)).thenReturn(user());

        ResponseEntity<?> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void login_returnsOkWithLoginResponse() {
        LoginRequest request = new LoginRequest();
        LoginResponse loginResponse = new LoginResponse("temp-token", null, null);
        when(authService.login(request)).thenReturn(loginResponse);

        ResponseEntity<?> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void verifyOtp_returnsOkWithTokens() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        when(authService.verifyOtp(request)).thenReturn(new TokenResponse("a", "r", "remember"));

        ResponseEntity<?> response = controller.verifyOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void refresh_returnsOkWithNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        when(authService.refreshToken(request)).thenReturn(new TokenResponse("new-a", "r", null));

        ResponseEntity<?> response = controller.refresh(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_missingAuthHeader_returnsUnauthorized() {
        ResponseEntity<?> response = controller.logout(null, new LogoutRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(authService, never()).logout(anyString(), any());
    }

    @Test
    void logout_headerWithoutBearerPrefix_returnsUnauthorized() {
        ResponseEntity<?> response = controller.logout("token-without-prefix", new LogoutRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_invalidToken_returnsUnauthorized() {
        when(tokenProvider.validateToken("bad-token")).thenReturn(false);

        ResponseEntity<?> response = controller.logout("Bearer bad-token", new LogoutRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_denylistedToken_returnsUnauthorized() {
        when(tokenProvider.validateToken("used-token")).thenReturn(true);
        when(tokenDenylistRepository.isDenylisted("used-token")).thenReturn(true);

        ResponseEntity<?> response = controller.logout("Bearer used-token", new LogoutRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_validToken_callsServiceAndReturnsOk() {
        when(tokenProvider.validateToken("good-token")).thenReturn(true);
        when(tokenDenylistRepository.isDenylisted("good-token")).thenReturn(false);
        LogoutRequest request = new LogoutRequest();

        ResponseEntity<?> response = controller.logout("Bearer good-token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).logout("good-token", request);
    }

    @Test
    void listUsers_returnsOkWithUserList() {
        when(authService.listUsers()).thenReturn(List.of(user()));

        ResponseEntity<?> response = controller.listUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateUser_returnsOkWithUpdatedUser() {
        UpdateUserRequest request = new UpdateUserRequest();
        when(authService.updateUser("user-1", request)).thenReturn(user());

        ResponseEntity<?> response = controller.updateUser("user-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteUser_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteUser("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).deleteUser("user-1");
    }
}
