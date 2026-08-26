package online.lifeasgame.inventory.application.command;

import online.lifeasgame.core.support.IdGenerator;

import java.util.regex.Pattern;

public final class AdminInventoryEntitlementCommand {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._:-]*"
    );

    private AdminInventoryEntitlementCommand() {
    }

    public record AddToInventory(
            Long playerId,
            Long itemId,
            int quantity,
            boolean bound,
            String reason,
            String idempotencyKey,
            String correlationId
    ) {
        public AddToInventory {
            playerId = requirePositive(playerId, "playerId");
            itemId = requirePositive(itemId, "itemId");
            quantity = requirePositive(quantity, "quantity");
            reason = requireReason(reason);
            idempotencyKey = requireIdentifier(
                    idempotencyKey,
                    "idempotencyKey",
                    128
            );
            correlationId = resolveCorrelationId(correlationId);
        }
    }

    public record DeliverToMailbox(
            Long playerId,
            Long itemId,
            int quantity,
            boolean bound,
            String reason,
            String idempotencyKey,
            String correlationId
    ) {
        public DeliverToMailbox {
            playerId = requirePositive(playerId, "playerId");
            itemId = requirePositive(itemId, "itemId");
            quantity = requirePositive(quantity, "quantity");
            reason = requireReason(reason);
            idempotencyKey = requireIdentifier(
                    idempotencyKey,
                    "idempotencyKey",
                    128
            );
            correlationId = resolveCorrelationId(correlationId);
        }
    }

    private static Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static String requireReason(String value) {
        String reason = requireText(value, "reason", 512);
        if (reason.codePoints().anyMatch(
                AdminInventoryEntitlementCommand::isUnsafeReasonCharacter
        )) {
            throw new IllegalArgumentException(
                    "reason must be a single-line operational rationale"
            );
        }
        if (reason.codePoints().noneMatch(
                AdminInventoryEntitlementCommand::isVisibleReasonCharacter
        )) {
            throw new IllegalArgumentException(
                    "reason must contain a visible character"
            );
        }
        return reason;
    }

    private static boolean isUnsafeReasonCharacter(int value) {
        int type = Character.getType(value);
        return Character.isISOControl(value)
                || type == Character.FORMAT
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR;
    }

    private static boolean isVisibleReasonCharacter(int value) {
        int type = Character.getType(value);
        return type != Character.FORMAT
                && type != Character.SPACE_SEPARATOR;
    }

    private static String resolveCorrelationId(String correlationId) {
        return correlationId == null
                ? IdGenerator.newTraceId()
                : requireIdentifier(correlationId, "correlationId", 100);
    }

    private static String requireIdentifier(
            String value,
            String field,
            int max
    ) {
        String identifier = requireText(value, field, max);
        if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(field + " has an unsafe format");
        }
        return identifier;
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(
                    field + " must contain 1-" + max + " characters"
            );
        }
        return value.strip();
    }
}
