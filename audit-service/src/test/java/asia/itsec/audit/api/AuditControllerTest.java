package asia.itsec.audit.api;

import asia.itsec.audit.application.AuditService;
import asia.itsec.audit.domain.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock private AuditService auditService;

    @Test
    void getAllLogs_returnsOkWithLogs() {
        AuditController controller = new AuditController(auditService);
        when(auditService.getAllLogs()).thenReturn(List.of(AuditLog.builder().id("1").action("LOGIN_SUCCESS").build()));

        ResponseEntity<?> response = controller.getAllLogs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
