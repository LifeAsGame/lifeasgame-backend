package online.lifeasgame.adminaudit.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.domain.AdminAuditEvent;
import online.lifeasgame.adminaudit.domain.repository.AdminAuditEventRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class AdminAuditEventRepositoryAdapter implements AdminAuditEventRepository {

    private final JpaAdminAuditEventRepository jpa;

    @Override
    public AdminAuditEvent append(AdminAuditEvent event) {
        return jpa.saveAndFlush(event);
    }
}
