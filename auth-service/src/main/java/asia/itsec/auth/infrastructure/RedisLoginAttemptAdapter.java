package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisLoginAttemptAdapter implements LoginAttemptRepository {

    private static final String ATTEMPT_PREFIX = "login_attempts:";
    private static final String LOCK_PREFIX = "lock:";
    private static final int MAX_ATTEMPTS = 5;
    private static final int ATTEMPT_TTL_SECONDS = 600;
    private static final int LOCK_TTL_SECONDS = 1800;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void recordFailedAttempt(String userId) {
        String attemptKey = ATTEMPT_PREFIX + userId;
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, Duration.ofSeconds(ATTEMPT_TTL_SECONDS));
        }

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            String lockKey = LOCK_PREFIX + userId;
            redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofSeconds(LOCK_TTL_SECONDS));
            redisTemplate.delete(attemptKey);
        }
    }

    @Override
    public void resetAttempts(String userId) {
        redisTemplate.delete(ATTEMPT_PREFIX + userId);
    }

    @Override
    public boolean isLocked(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + userId));
    }

    @Override
    public long getLockoutRemainingSeconds(String userId) {
        Long expire = redisTemplate.getExpire(LOCK_PREFIX + userId);
        return expire != null && expire > 0 ? expire : 0;
    }
}
