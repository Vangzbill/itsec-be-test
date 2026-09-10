package asia.itsec.audit.infrastructure;

import asia.itsec.audit.domain.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDocument {

    @Id
    private String id;
    private String action;
    private String entityId;
    private Instant timestamp;

    public AuditLog toDomain() {
        return AuditLog.builder()
                .id(id)
                .action(action)
                .entityId(entityId)
                .timestamp(timestamp)
                .build();
    }

    public static AuditLogDocument fromDomain(AuditLog auditLog) {
        return AuditLogDocument.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityId(auditLog.getEntityId())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
