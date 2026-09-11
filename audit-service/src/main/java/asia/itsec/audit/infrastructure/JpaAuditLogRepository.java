package asia.itsec.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, String> {
}
