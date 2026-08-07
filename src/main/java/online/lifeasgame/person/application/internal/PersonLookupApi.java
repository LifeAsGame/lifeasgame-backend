package online.lifeasgame.person.application.internal;

import java.util.Map;
import java.util.Set;

public interface PersonLookupApi {

    PersonReference getOwnedActive(Long personId, Long ownerPlayerId);

    Map<Long, PersonReference> findOwnedByIds(
            Set<Long> personIds,
            Long ownerPlayerId
    );

    record PersonReference(
            Long id,
            Long linkedUserId,
            String displayName
    ) {
    }
}
