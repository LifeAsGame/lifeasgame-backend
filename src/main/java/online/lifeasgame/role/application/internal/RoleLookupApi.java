package online.lifeasgame.role.application.internal;

public interface RoleLookupApi {

    RoleReference getOwned(Long roleId, Long playerId);

    record RoleReference(Long id, String name, String status) {
    }
}
