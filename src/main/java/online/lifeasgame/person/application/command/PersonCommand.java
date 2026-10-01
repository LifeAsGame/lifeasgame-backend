package online.lifeasgame.person.application.command;

import online.lifeasgame.person.domain.PersonProfile;
import java.time.LocalDate;

public final class PersonCommand {

    private PersonCommand() {
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

    public record Update(
            String displayName,
            String notes,
            LocalDate birthday,
            String contact,
            PersonProfile profile,
            boolean profileProvided
    ) {
        public Update(String displayName, String notes, LocalDate birthday, String contact) {
            this(displayName, notes, birthday, contact, null, false);
        }
    }
}
