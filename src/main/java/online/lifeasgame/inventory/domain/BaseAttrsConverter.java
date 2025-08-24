package online.lifeasgame.inventory.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
class BaseAttrsConverter implements AttributeConverter<BaseAttrs, String> {

    private static final ObjectMapper M = new ObjectMapper();
    private static final TypeReference<Map<String, Integer>> T = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(BaseAttrs attr) {
        try {
            if (attr == null) {
                return null;
            }
            return M.writeValueAsString(attr.attrs());
        } catch (Exception e) {
            throw new IllegalArgumentException("serialize baseAttrs", e);
        }
    }

    @Override
    public BaseAttrs convertToEntityAttribute(String db) {
        if (db == null || db.isBlank()) {
            return BaseAttrs.empty();
        }
        try {
            return new BaseAttrs(M.readValue(db, T));
        } catch (Exception e) {
            throw new IllegalArgumentException("deserialize baseAttrs", e);
        }
    }
}
