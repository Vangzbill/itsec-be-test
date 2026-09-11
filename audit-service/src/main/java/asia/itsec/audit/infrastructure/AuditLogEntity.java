package asia.itsec.audit.infrastructure;

import asia.itsec.audit.domain.AuditLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    private String id;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_username")
    private String actorUsername;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    private String browser;

    private String os;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "request_path")
    private String requestPath;

    @Column(name = "http_method")
    private String httpMethod;

    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditLog toDomain() {
        return AuditLog.builder()
                .id(id)
                .actorId(actorId)
                .actorUsername(actorUsername)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .browser(browser)
                .os(os)
                .deviceType(deviceType)
                .requestPath(requestPath)
                .httpMethod(httpMethod)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    public static AuditLogEntity fromDomain(AuditLog auditLog) {
        return AuditLogEntity.builder()
                .id(auditLog.getId())
                .actorId(auditLog.getActorId())
                .actorUsername(auditLog.getActorUsername())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .browser(auditLog.getBrowser())
                .os(auditLog.getOs())
                .deviceType(auditLog.getDeviceType())
                .requestPath(auditLog.getRequestPath())
                .httpMethod(auditLog.getHttpMethod())
                .status(auditLog.getStatus())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
