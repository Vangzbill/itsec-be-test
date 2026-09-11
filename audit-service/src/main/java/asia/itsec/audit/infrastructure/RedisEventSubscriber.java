package asia.itsec.audit.infrastructure;

import asia.itsec.audit.application.AuditService;
import asia.itsec.shared.event.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RedisEventSubscriber implements MessageListener {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            AuditEvent event = objectMapper.readValue(payload, AuditEvent.class);
            auditService.recordLog(event);
        } catch (Exception e) {
            System.err.println("Error processing audit event: " + e.getMessage());
        }
    }
}
