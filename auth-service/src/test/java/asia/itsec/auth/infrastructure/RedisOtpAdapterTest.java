package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisOtpAdapterTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private PasswordEncoder passwordEncoder;

    private RedisOtpAdapter adapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adapter = new RedisOtpAdapter(redisTemplate, passwordEncoder, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void save_storesHashedOtpUnderTempTokenKey() {
        adapter.save("temp-token", "hashed-code", "user-1");

        verify(valueOperations).set(eq("mfa:temp-token"), anyString(), eq(Duration.ofSeconds(300)));
    }

    @Test
    void verify_correctCode_deletesKeyAndReturnsUserId() throws Exception {
        String json = objectMapper.writeValueAsString(otpData("hashed", "user-1", 0));
        when(valueOperations.get("mfa:temp-token")).thenReturn(json);
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        String userId = adapter.verify("temp-token", "123456");

        assertThat(userId).isEqualTo("user-1");
        verify(redisTemplate).delete("mfa:temp-token");
    }

    @Test
    void verify_missingKey_throwsExpiredOrInvalid() {
        when(valueOperations.get("mfa:missing")).thenReturn(null);

        assertThatThrownBy(() -> adapter.verify("missing", "123456"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("expired or invalid");
    }

    @Test
    void verify_wrongCode_incrementsAttemptsAndThrows() throws Exception {
        String json = objectMapper.writeValueAsString(otpData("hashed", "user-1", 0));
        when(valueOperations.get("mfa:temp-token")).thenReturn(json);
        when(passwordEncoder.matches("000000", "hashed")).thenReturn(false);
        when(redisTemplate.getExpire("mfa:temp-token")).thenReturn(200L);

        assertThatThrownBy(() -> adapter.verify("temp-token", "000000"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Attempts left");
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verify_thirdWrongAttempt_locksOutAndDeletesKey() throws Exception {
        String json = objectMapper.writeValueAsString(otpData("hashed", "user-1", 2));
        when(valueOperations.get("mfa:temp-token")).thenReturn(json);
        when(passwordEncoder.matches("000000", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> adapter.verify("temp-token", "000000"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Too many wrong OTP attempts");
        verify(redisTemplate).delete("mfa:temp-token");
    }

    private RedisOtpAdapter.OtpData otpData(String hashedCode, String userId, int attempts) {
        RedisOtpAdapter.OtpData data = new RedisOtpAdapter.OtpData();
        data.setHashedCode(hashedCode);
        data.setUserId(userId);
        data.setAttempts(attempts);
        return data;
    }
}
