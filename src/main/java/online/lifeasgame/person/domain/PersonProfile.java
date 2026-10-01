package online.lifeasgame.person.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.person.domain.error.PersonError;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonProfile(
        String nickname, String gender, Integer ageAtReference, LocalDate ageReferenceDate,
        String occupation, String organization, String area, String mbti,
        List<ContactChannel> contactChannels,
        List<String> hobbies, List<String> interests,
        List<String> favoriteFoods, List<String> avoidedFoods,
        List<String> favoriteAnimals, List<String> avoidedAnimals,
        List<String> favoriteMusic, List<String> favoriteMedia, List<String> favoriteActivities,
        List<String> conversationTopics, List<String> avoidTopics, List<String> giftIdeas,
        LocalDate firstMetOn, String metContext, LocalDate lastContactOn, String conversationNotes,
        List<ImportantDate> importantDates, List<CustomNote> customNotes
) {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());

    public static PersonProfile empty() {
        return normalized(null);
    }

    public static PersonProfile normalized(PersonProfile raw) {
        if (raw == null) {
            raw = new PersonProfile(null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null);
        }
        if ((raw.ageAtReference == null) != (raw.ageReferenceDate == null)
                || raw.ageAtReference != null && (raw.ageAtReference < 0 || raw.ageAtReference > 150)) {
            throw invalid();
        }
        PersonProfile result = new PersonProfile(
                text(raw.nickname, 80), text(raw.gender, 40), raw.ageAtReference, raw.ageReferenceDate,
                text(raw.occupation, 120), text(raw.organization, 120), text(raw.area, 120), text(raw.mbti, 16),
                entries(raw.contactChannels, 10, ContactChannel::normalized),
                tags(raw.hobbies), tags(raw.interests), tags(raw.favoriteFoods), tags(raw.avoidedFoods),
                tags(raw.favoriteAnimals), tags(raw.avoidedAnimals), tags(raw.favoriteMusic),
                tags(raw.favoriteMedia), tags(raw.favoriteActivities), tags(raw.conversationTopics),
                tags(raw.avoidTopics), tags(raw.giftIdeas), raw.firstMetOn,
                text(raw.metContext, 2000), raw.lastContactOn, text(raw.conversationNotes, 2000),
                entries(raw.importantDates, 20, ImportantDate::normalized),
                entries(raw.customNotes, 20, CustomNote::normalized)
        );
        if (result.toJson().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65536) {
            throw invalid();
        }
        return result;
    }

    public String toJson() {
        try {
            return JSON.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize Person profile", e);
        }
    }

    public static PersonProfile fromJson(String json) {
        if (json == null) return empty();
        try {
            return normalized(JSON.readValue(json, PersonProfile.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read stored Person profile", e);
        }
    }

    private static String text(String value, int limit) {
        if (value == null || value.isBlank()) return null;
        String result = value.strip();
        if (result.length() > limit) throw invalid();
        return result;
    }

    private static String required(String value, int limit) {
        String result = text(value, limit);
        if (result == null) throw invalid();
        return result;
    }

    private static List<String> tags(List<String> values) {
        if (values == null) return List.of();
        if (values.size() > 20) throw invalid();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String tag = text(value, 100);
            if (tag != null) result.add(tag);
        }
        return List.copyOf(result);
    }

    private static <T> List<T> entries(List<T> values, int limit, java.util.function.Function<T, T> normalize) {
        if (values == null) return List.of();
        if (values.size() > limit || values.stream().anyMatch(java.util.Objects::isNull)) throw invalid();
        return values.stream().map(normalize).toList();
    }

    private static DomainException invalid() {
        return new DomainException(PersonError.INVALID_PERSON_PROFILE);
    }

    public enum ContactKind { PHONE, EMAIL, MESSENGER, SOCIAL, OTHER }

    public record ContactChannel(ContactKind kind, String label, String value) {
        private ContactChannel normalized() {
            if (kind == null) throw invalid();
            return new ContactChannel(kind, text(label, 40), required(value, 200));
        }
    }

    public record ImportantDate(String label, LocalDate date, Boolean repeatYearly) {
        private ImportantDate normalized() {
            if (date == null || repeatYearly == null) throw invalid();
            return new ImportantDate(required(label, 80), date, repeatYearly);
        }
    }

    public record CustomNote(String label, String value) {
        private CustomNote normalized() {
            return new CustomNote(required(label, 80), required(value, 1000));
        }
    }
}
