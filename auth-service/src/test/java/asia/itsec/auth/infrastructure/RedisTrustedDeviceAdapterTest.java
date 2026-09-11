package asia.itsec.auth.infrastructure;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisTrustedDeviceAdapterTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RedisTrustedDeviceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisTrustedDeviceAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void issue_storesUserIdUnderRandomTokenWithThirtyDayTtl() {
        String token = adapter.issue("user-1");

        assertThat(token).isNotBlank();
        verify(valueOperations).set(eq("trusted-device:" + token), eq("user-1"), eq(Duration.ofDays(30)));
    }

    @Test
    void resolve_returnsUserIdForToken() {
        when(valueOperations.get("trusted-device:some-token")).thenReturn("user-1");

        String result = adapter.resolve("some-token");

        assertThat(result).isEqualTo("user-1");
    }

    @Test
    void resolve_unknownToken_returnsNull() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThat(adapter.resolve("unknown")).isNull();
    }
}
