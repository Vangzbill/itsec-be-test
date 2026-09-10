package asia.itsec.auth.infrastructure;

import asia.itsec.auth.domain.OtpRepository;
import asia.itsec.auth.domain.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisOtpAdapter implements OtpRepository {

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private static final String PREFIX = "mfa:";
    private static final int TTL_SECONDS = 300; // 5 mins

    @Data
    public static class OtpData {
        private String hashedCode;
        private String userId;
        private int attempts;
    }

    @Override
    public void save(String tempToken, String hashedCode, String userId) {
        try {
            OtpData data = new OtpData();
            data.setHashedCode(hashedCode);
            data.setUserId(userId);
            data.setAttempts(0);
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(PREFIX + tempToken, json, Duration.ofSeconds(TTL_SECONDS));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save OTP", e);
        }
    }

    @Override
    public String verify(String tempToken, String rawCode) {
        String key = PREFIX + tempToken;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new SecurityException("OTP expired or invalid");
        }

        try {
            OtpData data = objectMapper.readValue(json, OtpData.class);
            if (passwordEncoder.matches(rawCode, data.getHashedCode())) {
                redisTemplate.delete(key);
                return data.getUserId();
            }

            data.setAttempts(data.getAttempts() + 1);
            if (data.getAttempts() >= 3) {
                redisTemplate.delete(key);
                throw new SecurityException("Too many wrong OTP attempts. Please login again.");
            }

            Long remainingTime = redisTemplate.getExpire(key);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), 
                    Duration.ofSeconds(remainingTime != null && remainingTime > 0 ? remainingTime : TTL_SECONDS));
            
            throw new SecurityException("Incorrect OTP. Attempts left: " + (3 - data.getAttempts()));
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new RuntimeException("Error verifying OTP", e);
        }
    }
}
