package online.lifeasgame.person.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import online.lifeasgame.person.domain.PersonProfile;

import java.time.LocalDate;

public final class PersonRequest {

    private PersonRequest() {
    }

    public record Create(
            String displayName,
            String notes,
            LocalDate birthday,
            String contact,
            PersonProfile profile
    ) {
        public Create(String displayName, String notes, LocalDate birthday, String contact) {
            this(displayName, notes, birthday, contact, null);
        }
    }

    public static final class Update {
        @JsonProperty private String displayName;
        @JsonProperty private String notes;
        @JsonProperty private LocalDate birthday;
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
    }
}
