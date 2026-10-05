package online.lifeasgame.person.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import online.lifeasgame.core.time.CalendarDateRange;
import online.lifeasgame.person.domain.PersonProfile;
import online.lifeasgame.platform.web.validation.DatabaseCalendarDate;

import java.time.LocalDate;

public final class PersonRequest {

    private PersonRequest() {
    }

    public record Create(
            String displayName,
            String notes,
            @DatabaseCalendarDate LocalDate birthday,
            String contact,
            PersonProfile profile
    ) {
        public Create(String displayName, String notes, LocalDate birthday, String contact) {
            this(displayName, notes, birthday, contact, null);
        }

        @JsonIgnore @AssertTrue(message = "Profile contains an invalid calendar date")
        public boolean isProfileDatesValid() { return profileDatesValid(profile); }
    }

    public static final class Update {
        @JsonProperty private String displayName;
        @JsonProperty private String notes;
        @JsonProperty @DatabaseCalendarDate private LocalDate birthday;
        @JsonProperty private String contact;
        @JsonProperty private PersonProfile profile;
        @JsonIgnore private boolean profileProvided;

        public Update() {
        }

        public Update(String displayName, String notes, LocalDate birthday, String contact) {
            this.displayName = displayName;
            this.notes = notes;
            this.birthday = birthday;
            this.contact = contact;
        }

        @JsonSetter("profile")
        public void setProfile(PersonProfile profile) {
            this.profile = profile;
            this.profileProvided = true;
        }

        public String displayName() { return displayName; }
        public String notes() { return notes; }
        public LocalDate birthday() { return birthday; }
        public String contact() { return contact; }
        public PersonProfile profile() { return profile; }
        @JsonIgnore public boolean profileProvided() { return profileProvided; }
        @JsonIgnore @AssertTrue(message = "Profile contains an invalid calendar date")
        public boolean isProfileDatesValid() { return profileDatesValid(profile); }
    }

    private static boolean profileDatesValid(PersonProfile profile) {
        return profile == null || CalendarDateRange.inJson(profile.ageReferenceDate())
                && CalendarDateRange.inJson(profile.firstMetOn())
                && CalendarDateRange.inJson(profile.lastContactOn())
                && (profile.importantDates() == null || profile.importantDates().stream()
                        .allMatch(date -> date == null || CalendarDateRange.inJson(date.date())));
    }
}
