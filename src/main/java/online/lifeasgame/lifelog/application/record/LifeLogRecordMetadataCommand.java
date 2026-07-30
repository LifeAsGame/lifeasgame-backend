package online.lifeasgame.lifelog.application.record;

import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;

public record LifeLogRecordMetadataCommand(
        String lifeLogSubtype,
        String reflectionScope
) {

    public static LifeLogRecordMetadataCommand none() {
        return new LifeLogRecordMetadataCommand(null, null);
    }

    public boolean isPresent() {
        return lifeLogSubtype != null || reflectionScope != null;
    }

    public Resolved resolve() {
        if (!isPresent()) {
            return new Resolved(null, null);
        }
        LifeLogSubtype subtype = LifeLogSubtype.parse(lifeLogSubtype);
        LifeLogReflectionScope scope =
                LifeLogReflectionScope.parse(reflectionScope);
        return new Resolved(subtype, scope);
    }

    public record Resolved(
            LifeLogSubtype subtype,
            LifeLogReflectionScope reflectionScope
    ) {
    }
}
