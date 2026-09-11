package asia.itsec.auth.api;

import asia.itsec.auth.application.*;
import asia.itsec.auth.domain.TokenDenylistRepository;
import asia.itsec.auth.domain.TokenProvider;
import asia.itsec.auth.domain.User;
import asia.itsec.shared.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenProvider tokenProvider;
    private final TokenDenylistRepository tokenDenylistRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<User>builder()
                        .success(true)
                        .message("Registration successful")
                        .data(user)
                        .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .success(true)
                .message("OTP sent to email")
                .data(response)
                .build());
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        TokenResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                .success(true)
                .message("OTP verification successful")
                .data(response)
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                .success(true)
                .message("Token refresh successful")
                .data(response)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody LogoutRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);

        if (!tokenProvider.validateToken(token) || tokenDenylistRepository.isDenylisted(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authService.logout(token, request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Logged out successfully")
                .build());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.<List<User>>builder()
                .success(true)
                .message("Users retrieved successfully")
                .data(authService.listUsers())
                .build());
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.<User>builder()
                .success(true)
                .message("User updated successfully")
                .data(authService.updateUser(id, request))
                .build());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
