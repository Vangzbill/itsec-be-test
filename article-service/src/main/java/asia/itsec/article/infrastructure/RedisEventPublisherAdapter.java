package asia.itsec.article.infrastructure;

import asia.itsec.article.domain.EventPublisher;
import asia.itsec.shared.event.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisEventPublisherAdapter implements EventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CHANNEL = "AUDIT_EVENTS";

    @Override
    public void publish(AuditEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish audit event", e);
        }
    }
}
