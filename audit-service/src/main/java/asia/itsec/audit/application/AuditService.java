package asia.itsec.audit.application;

import asia.itsec.audit.domain.AuditLog;
import asia.itsec.audit.domain.AuditLogRepository;
import asia.itsec.shared.event.AuditEvent;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void recordLog(AuditEvent event) {
        String browser = null;
        String os = null;
        String deviceType = null;

        if (event.getUserAgent() != null && !event.getUserAgent().isBlank()) {
            UserAgent userAgent = UserAgent.parseUserAgentString(event.getUserAgent());
            browser = userAgent.getBrowser().getName();
            os = userAgent.getOperatingSystem().getName();
            deviceType = userAgent.getOperatingSystem().getDeviceType().getName();
        }

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID().toString())
                .actorId(event.getActorId())
                .actorUsername(event.getActorUsername())
                .action(event.getAction())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .browser(browser)
                .os(os)
                .deviceType(deviceType)
                .requestPath(event.getRequestPath())
                .httpMethod(event.getHttpMethod())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now())
                .build();

        auditLogRepository.save(log);
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }
}
