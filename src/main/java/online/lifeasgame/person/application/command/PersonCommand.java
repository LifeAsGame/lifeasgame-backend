package online.lifeasgame.person.application.command;

import java.time.LocalDate;

public final class PersonCommand {

    private PersonCommand() {
    }

    public record Create(
            String displayName,
            String notes,
            LocalDate birthday,
            String contact
    ) {
    }

    public record Update(
            String displayName,
            String notes,
            LocalDate birthday,
            String contact
    ) {
    }
}
