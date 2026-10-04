package online.lifeasgame.platform.web.validation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import online.lifeasgame.core.time.CalendarDateRange;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class CalendarInstantDeserializer extends JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value != null && value.matches("\\d{4}-\\d{2}-\\d{2}T.+")) {
            try {
                Instant instant = OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
                if (CalendarDateRange.inDateTimeColumn(instant)) return instant;
            } catch (DateTimeException ignored) {
                // The normal bad-input handler maps this invalid JSON value to HTTP 400.
            }
        }
        throw InvalidFormatException.from(parser, "Invalid calendar instant", value, Instant.class);
    }
}
