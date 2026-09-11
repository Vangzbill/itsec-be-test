package asia.itsec.audit.application;

import asia.itsec.audit.domain.AuditLog;
import asia.itsec.audit.domain.AuditLogRepository;
import asia.itsec.shared.event.AuditEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @Test
    void recordLog_parsesBrowserAndOsFromUserAgent() {
        AuditService auditService = new AuditService(auditLogRepository);

        AuditEvent event = AuditEvent.builder()
                .actorId("user-1")
                .actorUsername("testuser")
                .action("LOGIN_SUCCESS")
                .entityType("USER")
                .entityId("user-1")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .requestPath("/auth/login")
                .httpMethod("POST")
                .status("SUCCESS")
                .createdAt(Instant.now())
                .build();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        auditService.recordLog(event);

        AuditLog saved = captor.getValue();
        assertThat(saved.getBrowser()).containsIgnoringCase("chrome");
        assertThat(saved.getOs()).containsIgnoringCase("windows");
        assertThat(saved.getDeviceType()).isNotBlank();
        assertThat(saved.getActorUsername()).isEqualTo("testuser");
        assertThat(saved.getAction()).isEqualTo("LOGIN_SUCCESS");
    }

    @Test
    void recordLog_withBlankUserAgent_doesNotFail() {
        AuditService auditService = new AuditService(auditLogRepository);

        AuditEvent event = AuditEvent.builder()
                .actorId("user-1")
                .action("LOGOUT")
                .entityType("USER")
                .entityId("user-1")
                .userAgent(null)
                .createdAt(Instant.now())
                .build();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        auditService.recordLog(event);

        AuditLog saved = captor.getValue();
        assertThat(saved.getBrowser()).isNull();
        assertThat(saved.getOs()).isNull();
        assertThat(saved.getDeviceType()).isNull();
    }

    @Test
    void recordLog_withMissingCreatedAt_defaultsToNow() {
        AuditService auditService = new AuditService(auditLogRepository);

        AuditEvent event = AuditEvent.builder()
                .actorId("user-1")
                .action("LOGOUT")
                .createdAt(null)
                .build();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        auditService.recordLog(event);

        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void getAllLogs_returnsFromRepository() {
        AuditService auditService = new AuditService(auditLogRepository);
        AuditLog log = AuditLog.builder().id("1").action("LOGIN_SUCCESS").build();
        when(auditLogRepository.findAll()).thenReturn(List.of(log));

        List<AuditLog> result = auditService.getAllLogs();

        assertThat(result).containsExactly(log);
    }
}
