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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisTokenDenylistAdapterTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RedisTokenDenylistAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisTokenDenylistAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void denylist_storesTokenWithGivenTtl() {
        adapter.denylist("access-token", 120L);

        verify(valueOperations).set("denylist:access-token", "revoked", Duration.ofSeconds(120));
    }

    @Test
    void isDenylisted_whenKeyExists_returnsTrue() {
        when(redisTemplate.hasKey("denylist:access-token")).thenReturn(true);
        assertThat(adapter.isDenylisted("access-token")).isTrue();
    }

    @Test
    void isDenylisted_whenKeyMissing_returnsFalse() {
        when(redisTemplate.hasKey("denylist:access-token")).thenReturn(false);
        assertThat(adapter.isDenylisted("access-token")).isFalse();
    }
}
