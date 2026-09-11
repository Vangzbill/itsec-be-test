package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.TrustedDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisTrustedDeviceAdapter implements TrustedDeviceRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "trusted-device:";
    private static final int TTL_SECONDS = 30 * 24 * 60 * 60; // 30 days

    @Override
    public String issue(String userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + token, userId, Duration.ofSeconds(TTL_SECONDS));
        return token;
    }

    @Override
    public String resolve(String rememberToken) {
        return redisTemplate.opsForValue().get(PREFIX + rememberToken);
    }
}
