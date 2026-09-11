package asia.itsec.article.domain;

import asia.itsec.shared.event.AuditEvent;

public interface EventPublisher {
    void publish(AuditEvent event);
}
