package asia.itsec.auth.infrastructure;

import asia.itsec.shared.event.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisEventPublisherAdapterTest {

    @Mock private StringRedisTemplate redisTemplate;

    @Test
    void publish_sendsSerializedEventToAuditChannel() {
        RedisEventPublisherAdapter adapter = new RedisEventPublisherAdapter(redisTemplate, new ObjectMapper().registerModule(new JavaTimeModule()));

        AuditEvent event = AuditEvent.builder()
                .actorId("user-1").action("LOGIN_SUCCESS").entityType("USER").entityId("user-1")
                .createdAt(Instant.now()).build();

        adapter.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("AUDIT_EVENTS"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("LOGIN_SUCCESS").contains("user-1");
    }
}
