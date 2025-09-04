package online.lifeasgame.character.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderTypeConverter implements AttributeConverter<GenderType, String> {

    @Override
    public String convertToDatabaseColumn(GenderType a) {
        return a == null ? null : a.name().toLowerCase();
    }

    @Override
    public GenderType convertToEntityAttribute(String db) {
        return db == null ? null : GenderType.parse(db);
    }
}
