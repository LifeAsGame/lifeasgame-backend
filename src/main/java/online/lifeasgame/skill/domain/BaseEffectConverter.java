package online.lifeasgame.skill.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
public class BaseEffectConverter implements AttributeConverter<BaseEffect, String> {

    private static final ObjectMapper M = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(BaseEffect attr) {
        try {
            return M.writeValueAsString(attr == null ? BaseEffect.of(Map.of()) : attr);
        } catch (Exception e) {
            throw new IllegalArgumentException("baseEffect to json", e);
        }
    }

    @Override
    public BaseEffect convertToEntityAttribute(String db) {
        if (db == null || db.isBlank()) {
            return BaseEffect.of(Map.of());
        }
        try {
            var map = M.readValue(db, new TypeReference<Map<String, Integer>>() {});
            return BaseEffect.of(map);
        } catch (Exception e) {
            throw new IllegalArgumentException("json to baseEffect", e);
        }
    }
}
