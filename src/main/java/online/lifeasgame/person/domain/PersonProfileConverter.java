package online.lifeasgame.person.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PersonProfileConverter implements AttributeConverter<PersonProfile, String> {
    @Override
    public String convertToDatabaseColumn(PersonProfile profile) {
        return profile == null ? null : profile.toJson();
    }

    @Override
    public PersonProfile convertToEntityAttribute(String json) {
        return PersonProfile.fromJson(json);
    }
}
