package online.lifeasgame.character.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import online.lifeasgame.character.domain.StatusEffectCode;
import online.lifeasgame.character.domain.StatusEffects;

@Converter
public class StatusEffectsEnumConverter implements AttributeConverter<StatusEffects, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(StatusEffects attribute) {
        try {
            if (attribute == null) return null;
            var names = attribute.asList().stream().map(Enum::name).toList();
            return MAPPER.writeValueAsString(names);
        } catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    @Override
    public StatusEffects convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return StatusEffects.empty();
            List<String> names = MAPPER.readValue(dbData, TYPE);
            return StatusEffects.of(names.stream().map(StatusEffectCode::parse).toList());
        } catch (Exception e) { throw new IllegalArgumentException(e); }
    }
}
