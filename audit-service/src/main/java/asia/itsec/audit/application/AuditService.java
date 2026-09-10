package asia.itsec.audit.application;

import asia.itsec.audit.domain.AuditLog;
import asia.itsec.audit.domain.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void recordLog(String action, String entityId) {
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID().toString())
                .action(action)
                .entityId(entityId)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }
}
