package online.lifeasgame.economy.application.command;

import online.lifeasgame.core.support.IdGenerator;

import java.util.regex.Pattern;

public record AdminWalletAdjustmentCommand(
        Long playerId,
        long amount,
        String currency,
        boolean debit,
        String reason,
        String idempotencyKey,
        String correlationId
) {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._:-]*"
    );

    public AdminWalletAdjustmentCommand {
        if (playerId == null || playerId <= 0) {
            throw new IllegalArgumentException("playerId must be positive");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }
        currency = requireText(currency, "currency", 10);
        reason = requireReason(reason);
        idempotencyKey = requireIdentifier(
                idempotencyKey,
                "idempotencyKey",
                128
        );
        correlationId = correlationId == null
                ? IdGenerator.newTraceId()
                : requireIdentifier(correlationId, "correlationId", 100);
    }

    private static String requireReason(String value) {
        String reason = requireText(value, "reason", 512);
        if (reason.codePoints().anyMatch(
                AdminWalletAdjustmentCommand::isUnsafeReasonCharacter
        )) {
            throw new IllegalArgumentException(
                    "reason must be a single-line operational rationale"
            );
        }
        return reason;
    }

    private static boolean isUnsafeReasonCharacter(int value) {
        int type = Character.getType(value);
        return Character.isISOControl(value)
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR;
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
