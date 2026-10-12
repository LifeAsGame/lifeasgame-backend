package online.lifeasgame.role.application.internal;

public interface RoleLookupApi {

    RoleReference getOwned(Long roleId, Long playerId);

    RoleReference getOwnedActiveForUpdate(Long roleId, Long playerId);

    record RoleReference(Long id, String name, String status, String roleType) {
        public RoleReference(Long id, String name, String status) {
            this(id, name, status, null);
        }
    }
}
