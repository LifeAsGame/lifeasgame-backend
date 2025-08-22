package online.lifeasgame.character.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class StatusEffectsConverter implements AttributeConverter<StatusEffects, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    @Override public String convertToDatabaseColumn(StatusEffects attribute) {
        try {
            if (attribute == null) {
                return null;
            }
            return MAPPER.writeValueAsString(attribute.asList());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    @Override public StatusEffects convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return StatusEffects.empty();
            }
            return StatusEffects.of(MAPPER.readValue(dbData, TYPE));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
