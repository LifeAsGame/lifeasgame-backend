package online.lifeasgame.skill.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
public class BaseEffectConverter implements AttributeConverter<BaseEffect, String> {

    private static final ObjectMapper M = new ObjectMapper();
    private static final TypeReference<Map<String, Integer>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(BaseEffect attr) {
        try {
            if (attr == null) {
                return null;
            }
            return M.writeValueAsString(attr.stats());
        } catch (Exception e) {
            throw new IllegalArgumentException("serialize baseEffect", e);
        }
    }

    @Override
    public BaseEffect convertToEntityAttribute(String db) {
        if (db == null || db.isBlank()) {
            return BaseEffect.empty();
        }
        try {
            Map<String, Integer> m = M.readValue(db, MAP_TYPE);
            return BaseEffect.of(m);
        } catch (Exception e) {
            throw new IllegalArgumentException("json to baseEffect", e);
        }
    }
}
