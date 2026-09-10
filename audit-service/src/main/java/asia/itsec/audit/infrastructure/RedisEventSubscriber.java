package asia.itsec.audit.infrastructure;

import asia.itsec.audit.application.AuditService;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            String event = jsonNode.get("event").asText();
            String entityId = jsonNode.get("articleId").asText();
            
            auditService.recordLog(event, entityId);
            
        } catch (Exception e) {
            System.err.println("Error processing Redis message: " + e.getMessage());
        }
    }
}
