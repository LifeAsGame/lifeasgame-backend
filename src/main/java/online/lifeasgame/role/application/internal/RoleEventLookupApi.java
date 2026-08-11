package online.lifeasgame.role.application.internal;

public interface RoleEventLookupApi {

    RoleEventReference getOwned(Long roleEventId, Long playerId);

    record RoleEventReference(Long id, Long roleId, String status) {
    }
}
