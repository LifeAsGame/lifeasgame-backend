package online.lifeasgame.inventory.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
class InstanceAttrsConverter implements AttributeConverter<InstanceAttrs, String> {

    private static final ObjectMapper M = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> T = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(InstanceAttrs attr) {
        try {
            if (attr == null) {
                return null;
            }
            return M.writeValueAsString(attr.attrs());
        } catch (Exception e) {
            throw new IllegalArgumentException("serialize instAttrs", e);
        }
    }

    @Override
    public InstanceAttrs convertToEntityAttribute(String db) {
        if (db == null || db.isBlank()) {
            return InstanceAttrs.empty();
        }
        try {
            return new InstanceAttrs(M.readValue(db, T));
        } catch (Exception e) {
            throw new IllegalArgumentException("deserialize instAttrs", e);
        }
    }
}
