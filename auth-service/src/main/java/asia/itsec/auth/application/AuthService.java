package asia.itsec.auth.application;

import asia.itsec.auth.domain.PasswordEncoder;
import asia.itsec.auth.domain.Role;
import asia.itsec.auth.domain.User;
import asia.itsec.auth.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    public User login(LoginRequest request) {
        String genericFailureMessage = "Invalid credentials";

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new SecurityException(genericFailureMessage));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new SecurityException(genericFailureMessage);
        }

        return user;
    }
}
