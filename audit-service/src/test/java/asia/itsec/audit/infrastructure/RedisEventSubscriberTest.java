package asia.itsec.audit.infrastructure;

import asia.itsec.audit.application.AuditService;
import asia.itsec.shared.event.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisEventSubscriberTest {

    @Mock private AuditService auditService;
    @Mock private Message message;

    @Test
    void onMessage_validPayload_recordsAuditEvent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisEventSubscriber subscriber = new RedisEventSubscriber(auditService, objectMapper);

        AuditEvent event = AuditEvent.builder()
                .actorId("user-1").action("LOGIN_SUCCESS").entityType("USER").entityId("user-1")
                .createdAt(Instant.now()).build();
        String json = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(message, null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).recordLog(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("LOGIN_SUCCESS");
    }

    @Test
    void onMessage_malformedPayload_doesNotThrowAndSkipsRecording() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisEventSubscriber subscriber = new RedisEventSubscriber(auditService, objectMapper);

        when(message.getBody()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(message, null);

        verify(auditService, never()).recordLog(org.mockito.ArgumentMatchers.any());
    }
}
