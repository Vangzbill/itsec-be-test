package asia.itsec.auth.api;

import asia.itsec.auth.application.*;
import asia.itsec.auth.domain.User;
import asia.itsec.shared.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
                .message("Login successful")
                .data(response)
                .build());
    }
}
