package asia.itsec.auth.api;

import asia.itsec.auth.application.AuthService;
import asia.itsec.auth.application.LoginRequest;
import asia.itsec.auth.application.RegisterRequest;
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
    public ResponseEntity<ApiResponse<User>> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request);
        return ResponseEntity.ok(ApiResponse.<User>builder()
                .success(true)
                .message("Login successful")
                .data(user)
                .build());
    }
}
