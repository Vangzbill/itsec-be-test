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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisLoginAttemptAdapterTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RedisLoginAttemptAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisLoginAttemptAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void recordFailedAttempt_firstAttempt_setsTenMinuteExpiry() {
        when(valueOperations.increment("login_attempts:user-1")).thenReturn(1L);

        adapter.recordFailedAttempt("user-1");

        verify(redisTemplate).expire("login_attempts:user-1", Duration.ofSeconds(600));
    }

    @Test
    void recordFailedAttempt_belowMaxAttempts_doesNotLockAccount() {
        when(valueOperations.increment("login_attempts:user-1")).thenReturn(3L);

        adapter.recordFailedAttempt("user-1");

        verify(valueOperations, never()).set(eq("lock:user-1"), anyString(), any(Duration.class));
        verify(redisTemplate, never()).delete("login_attempts:user-1");
    }

    @Test
    void recordFailedAttempt_reachesFiveAttempts_locksAccountForThirtyMinutes() {
        when(valueOperations.increment("login_attempts:user-1")).thenReturn(5L);

        adapter.recordFailedAttempt("user-1");

        verify(valueOperations).set("lock:user-1", "LOCKED", Duration.ofSeconds(1800));
        verify(redisTemplate).delete("login_attempts:user-1");
    }

    @Test
    void isLocked_whenLockKeyExists_returnsTrue() {
        when(redisTemplate.hasKey("lock:user-1")).thenReturn(true);

        boolean locked = adapter.isLocked("user-1");

        org.assertj.core.api.Assertions.assertThat(locked).isTrue();
    }

    @Test
    void isLocked_whenLockKeyMissing_returnsFalse() {
        when(redisTemplate.hasKey("lock:user-1")).thenReturn(false);

        boolean locked = adapter.isLocked("user-1");

        org.assertj.core.api.Assertions.assertThat(locked).isFalse();
    }

    @Test
    void getLockoutRemainingSeconds_returnsTtlFromRedis() {
        when(redisTemplate.getExpire("lock:user-1")).thenReturn(900L);

        long remaining = adapter.getLockoutRemainingSeconds("user-1");

        org.assertj.core.api.Assertions.assertThat(remaining).isEqualTo(900L);
    }

    @Test
    void resetAttempts_deletesAttemptCounter() {
        adapter.resetAttempts("user-1");

        verify(redisTemplate).delete("login_attempts:user-1");
    }
}
