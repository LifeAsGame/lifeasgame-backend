package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.repository.PlayerQuestRouteRepository;
import online.lifeasgame.quest.domain.repository.QuestRouteRepository;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
class BackendJourneyAccess {
    static final String ROUTE_CODE = "ROUTE_BACKEND_DEVELOPER_START";
    private static final Set<String> ROLE_TYPES = Set.of("ROLE_BACKEND_DEVELOPER", "ROLE_JOB_SEEKER");
    private final QuestRouteRepository routes;
    private final PlayerQuestRouteRepository playerRoutes;
    private final RoleLookupApi roles;

    static boolean isJourney(QuestCode code) {
        return code.name().startsWith("Q_DEV_");
    }

    static boolean compatible(String roleType) {
        return ROLE_TYPES.contains(roleType);
    }

    Long requireSelectedRole(Long playerId) {
        var route = routes.findByCode(ROUTE_CODE)
                .orElseThrow(() -> new DomainException(QuestError.JOURNEY_SELECTION_REQUIRED));
        var selected = playerRoutes.findByPlayerIdAndRouteId(playerId, route.getId())
                .orElseThrow(() -> new DomainException(QuestError.JOURNEY_SELECTION_REQUIRED));
        if (selected.getRoleId() == null) throw new DomainException(QuestError.JOURNEY_SELECTION_REQUIRED);
        var role = roles.getOwned(selected.getRoleId(), playerId);
        if (!"ACTIVE".equals(role.status()) || !compatible(role.roleType())) {
            throw new DomainException(QuestError.JOURNEY_ROLE_MISMATCH);
        }
        return role.id();
    }
}
