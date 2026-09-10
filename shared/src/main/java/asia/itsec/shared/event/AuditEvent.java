package asia.itsec.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String actorId;
    private String actorUsername;
    private String action;
    private String entityType;
    private String entityId;
    private String ipAddress;
    private String userAgent;
    private String requestPath;
    private String httpMethod;
    private String status;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
