package online.lifeasgame.adminaudit.application;

import online.lifeasgame.adminaudit.application.query.AdminAuditEventQuery;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

final class AdminAuditCursor {

    private AdminAuditCursor() {
    }

    static String encode(Instant occurredAt, Long id) {
        String value = occurredAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    static AdminAuditEventQuery.Cursor decode(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            String value = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            int separator = value.lastIndexOf('|');
            if (separator <= 0 || separator == value.length() - 1) {
                throw new IllegalArgumentException("Invalid audit cursor");
            }
            Instant occurredAt = Instant.parse(value.substring(0, separator));
            long id = Long.parseLong(value.substring(separator + 1));
            if (id <= 0) {
                throw new IllegalArgumentException("Invalid audit cursor");
            }
            return new AdminAuditEventQuery.Cursor(occurredAt, id);
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid audit cursor", exception);
        } catch (IllegalArgumentException exception) {
            if ("Invalid audit cursor".equals(exception.getMessage())) {
                throw exception;
            }
            throw new IllegalArgumentException("Invalid audit cursor", exception);
        }
    }
}
