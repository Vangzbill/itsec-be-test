package asia.itsec.audit.api;

import asia.itsec.audit.application.AuditService;
import asia.itsec.audit.domain.AuditLog;
import asia.itsec.shared.payload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAllLogs() {
        return ResponseEntity.ok(ApiResponse.<List<AuditLog>>builder()
                .success(true)
                .message("Audit logs retrieved successfully")
                .data(auditService.getAllLogs())
                .build());
    }
}
