package online.lifeasgame.lifelog.quick.application;

import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.WatchStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Component
public class QuickRecordRequestHasher {

    public String hash(QuickRecordCommand.Selected selected) {
        StringBuilder canonical = new StringBuilder();
        appendText(canonical, "type", selected.type().name());
        appendLifeLogMetadata(canonical, selected.lifeLogMetadata());
        switch (selected.type()) {
            case COLLECTION ->
                    appendCollection(canonical, selected.collection());
            case EXERCISE ->
                    appendExercise(canonical, selected.exercise());
            case MEDIA -> appendMedia(canonical, selected.media());
        }
        return sha256(canonical.toString());
    }

    private void appendLifeLogMetadata(
            StringBuilder target,
            LifeLogRecordMetadataCommand metadata
    ) {
        if (!metadata.isPresent()) {
            return;
        }
        LifeLogRecordMetadataCommand.Resolved resolved =
                metadata.resolve();
        appendOptionalText(
                target,
                "lifeLogSubtype",
                resolved.subtype() == null
                        ? null
                        : resolved.subtype().name()
        );
        appendOptionalText(
                target,
                "reflectionScope",
                resolved.reflectionScope() == null
                        ? null
                        : resolved.reflectionScope().name()
        );
        if (resolved.primaryRoleId() != null) {
            appendNumber(
                    target,
                    "primaryRoleId",
                    resolved.primaryRoleId()
            );
        }
        if (resolved.roleEventId() != null) {
            appendNumber(
                    target,
                    "roleEventId",
                    resolved.roleEventId()
            );
        }
    }

    private void appendCollection(
            StringBuilder target,
            CollectionCommand.Create value
    ) {
        appendText(
                target,
                "category",
                CollectionCategory.parse(value.category()).name()
        );
        appendRequiredText(target, "title", value.title());
        appendOptionalText(
                target,
                "originalTitle",
                value.originalTitle()
        );
        appendNumber(target, "quantity", value.quantity());
        appendOptionalText(
                target,
                "conditionNote",
                value.conditionNote()
        );
        appendOptionalText(
                target,
                "acquiredFrom",
                value.acquiredFrom()
        );
        appendTags(target, value.tags());
    }

    private void appendExercise(
            StringBuilder target,
            ExerciseCommand.Create value
    ) {
        appendText(
                target,
                "category",
                ExerciseCategory.parse(value.category()).name()
        );
        appendNumber(
                target,
                "durationMinutes",
                value.durationMinutes()
        );
        appendNumber(target, "distanceKm", value.distanceKm());
        appendNumber(target, "calories", value.calories());
        appendDate(target, "exercisedOn", value.exercisedOn());
        appendOptionalText(target, "memo", value.memo());
    }

    private void appendMedia(
            StringBuilder target,
            MediaLogCommand.Create value
    ) {
        appendText(
                target,
                "category",
                MediaCategory.parse(value.category()).name()
        );
        appendRequiredText(target, "title", value.title());
        appendOptionalText(
                target,
                "originalTitle",
                value.originalTitle()
        );
        appendNumber(
                target,
                "currentEpisode",
                value.currentEpisode()
        );
        appendNumber(target, "totalEpisode", value.totalEpisode());
        appendText(
                target,
                "status",
                WatchStatus.parse(value.status()).name()
        );
        appendTags(target, value.tags());
    }

    private void appendTags(StringBuilder target, Set<String> values) {
        TreeSet<String> normalized = new TreeSet<>();
        if (values != null) {
            values.stream()
                    .filter(value ->
                            value != null && !value.isBlank()
                    )
                    .map(value ->
                            value.trim().toLowerCase(Locale.ROOT)
                    )
                    .forEach(normalized::add);
        }
        appendNumber(target, "tagCount", normalized.size());
        normalized.forEach(tag -> appendText(target, "tag", tag));
    }

    private void appendRequiredText(
            StringBuilder target,
            String key,
            String value
    ) {
        if (value == null) {
            appendMissing(target, key);
            return;
        }
        appendText(target, key, value.trim());
    }

    private void appendOptionalText(
            StringBuilder target,
            String key,
            String value
    ) {
        if (value == null || value.isBlank()) {
            appendMissing(target, key);
            return;
        }
        appendText(target, key, value.trim());
    }

    private void appendDate(
            StringBuilder target,
            String key,
            LocalDate value
    ) {
        if (value == null) {
            appendMissing(target, key);
            return;
        }
        appendText(target, key, value.toString());
    }

    private void appendNumber(
            StringBuilder target,
            String key,
            Number value
    ) {
        if (value == null) {
            appendMissing(target, key);
            return;
        }
        String text = value.toString();
        appendKey(target, key);
        target.append("number:")
                .append(text.length())
                .append(':')
                .append(text)
                .append(';');
    }

    private void appendMissing(StringBuilder target, String key) {
        appendKey(target, key);
        target.append("missing;");
    }

    private void appendText(
            StringBuilder target,
            String key,
            String value
    ) {
        appendKey(target, key);
        target.append("text:")
                .append(value.length())
                .append(':')
                .append(value)
                .append(';');
    }

    private void appendKey(StringBuilder target, String key) {
        target.append(key.length())
                .append(':')
                .append(key)
                .append('=');
    }

    private String sha256(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }
}
