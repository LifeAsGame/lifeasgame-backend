package online.lifeasgame.role.application.internal;

import java.util.Collection;
import java.util.Map;

public interface RoleDisplayReadApi {

    Map<Long, String> findNames(Long playerId, Collection<Long> roleIds);
}
