package asia.itsec.audit.infrastructure;

import asia.itsec.audit.domain.AuditLog;
import asia.itsec.audit.domain.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final MongoAuditLogRepository mongoRepository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return mongoRepository.save(AuditLogDocument.fromDomain(auditLog)).toDomain();
    }

    @Override
    public List<AuditLog> findAll() {
        return mongoRepository.findAll().stream()
                .map(AuditLogDocument::toDomain)
                .collect(Collectors.toList());
    }
}
