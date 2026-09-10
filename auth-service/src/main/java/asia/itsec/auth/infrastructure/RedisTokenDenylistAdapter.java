package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.TokenDenylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisTokenDenylistAdapter implements TokenDenylistRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "denylist:";

    @Override
    public void denylist(String token, long remainingTtlSeconds) {
        redisTemplate.opsForValue().set(PREFIX + token, "revoked", Duration.ofSeconds(remainingTtlSeconds));
    }

    @Override
    public boolean isDenylisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
