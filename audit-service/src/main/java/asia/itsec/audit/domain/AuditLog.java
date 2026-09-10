package asia.itsec.audit.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditLog {
    private String id;
    private String action;
    private String entityId;
    private Instant timestamp;
}
