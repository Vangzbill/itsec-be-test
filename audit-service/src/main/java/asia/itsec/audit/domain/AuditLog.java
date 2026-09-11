package asia.itsec.audit.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditLog {
    private String id;
    private String actorId;
    private String actorUsername;
    private String action;
    private String entityType;
    private String entityId;
    private String ipAddress;
    private String userAgent;
    private String browser;
    private String os;
    private String deviceType;
    private String requestPath;
    private String httpMethod;
    private String status;
    private Instant createdAt;
}
