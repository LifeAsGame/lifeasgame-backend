package online.lifeasgame.lifelog.application.record;

import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;

public record LifeLogRecordMetadataCommand(
        String lifeLogSubtype,
        String reflectionScope,
        Long primaryRoleId,
        Long roleEventId
) {

    public LifeLogRecordMetadataCommand(
            String lifeLogSubtype,
            String reflectionScope
    ) {
        this(lifeLogSubtype, reflectionScope, null, null);
    }

    public static LifeLogRecordMetadataCommand none() {
        return new LifeLogRecordMetadataCommand(null, null, null, null);
    }

    public boolean isPresent() {
        return lifeLogSubtype != null
                || reflectionScope != null
                || primaryRoleId != null
                || roleEventId != null;
    }

    public Resolved resolve() {
        if (!isPresent()) {
            return new Resolved(null, null, null, null);
        }
        LifeLogSubtype subtype = lifeLogSubtype == null
                ? null
                : LifeLogSubtype.parse(lifeLogSubtype);
        LifeLogReflectionScope scope =
                LifeLogReflectionScope.parse(reflectionScope);
        if (subtype == null && scope != null) {
            throw new IllegalArgumentException(
                    "reflection metadata requires lifeLogSubtype"
            );
        }
        return new Resolved(
                subtype,
                scope,
                primaryRoleId,
                roleEventId
        );
    }

    public record Resolved(
            LifeLogSubtype subtype,
            LifeLogReflectionScope reflectionScope,
            Long primaryRoleId,
            Long roleEventId
    ) {
    }
}
