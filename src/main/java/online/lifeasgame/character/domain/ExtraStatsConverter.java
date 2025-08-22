package online.lifeasgame.character.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
public class ExtraStatsConverter implements AttributeConverter<ExtraStats, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Integer>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(ExtraStats attribute) {
        try {
            if (attribute == null) {
                return null;
            }
            return MAPPER.writeValueAsString(attribute.asMap());
        } catch (Exception e) {
            throw new IllegalArgumentException("serialize extraStats", e);
        }
    }

    @Override
    public ExtraStats convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return ExtraStats.empty();
            }
            Map<String, Integer> map = MAPPER.readValue(dbData, TYPE);

            return ExtraStats.of(map);
        } catch (Exception e) {
            throw new IllegalArgumentException("deserialize extraStats", e);
        }
    }
}
