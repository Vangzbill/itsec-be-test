package asia.itsec.audit.domain;

import java.util.List;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findAll();
}
