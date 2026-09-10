package asia.itsec.audit.infrastructure;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoAuditLogRepository extends MongoRepository<AuditLogDocument, String> {
}
