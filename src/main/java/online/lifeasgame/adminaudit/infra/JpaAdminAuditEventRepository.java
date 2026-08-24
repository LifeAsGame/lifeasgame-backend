package online.lifeasgame.adminaudit.infra;

import online.lifeasgame.adminaudit.domain.AdminAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaAdminAuditEventRepository
        extends JpaRepository<AdminAuditEvent, Long> {
}
