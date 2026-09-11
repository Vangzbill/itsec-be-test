package asia.itsec.auth.domain;

import asia.itsec.shared.event.AuditEvent;

public interface EventPublisher {
    void publish(AuditEvent event);
}
