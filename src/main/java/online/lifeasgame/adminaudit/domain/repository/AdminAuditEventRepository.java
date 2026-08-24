package online.lifeasgame.adminaudit.domain.repository;

import online.lifeasgame.adminaudit.domain.AdminAuditEvent;

public interface AdminAuditEventRepository {

    AdminAuditEvent append(AdminAuditEvent event);
}
