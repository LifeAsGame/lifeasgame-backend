package online.lifeasgame.person.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.person.domain.error.PersonError;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonTest {

    @Test
    void rejectsInvalidDisplayNameAndContactWithDomainErrors() {
        assertPersonError(
                () -> Person.create(1L, null, null, null, null),
                PersonError.INVALID_PERSON_DISPLAY_NAME
        );
        assertPersonError(
                () -> Person.create(1L, "   ", null, null, null),
                PersonError.INVALID_PERSON_DISPLAY_NAME
        );
        assertPersonError(
                () -> Person.create(1L, "x".repeat(81), null, null, null),
                PersonError.INVALID_PERSON_DISPLAY_NAME
        );
        assertPersonError(
                () -> Person.create(1L, "Alice", null, null, "x".repeat(121)),
                PersonError.INVALID_PERSON_CONTACT
        );
    }

    @Test
    void validatesLengthsAfterTrimmingAndNormalizesBlankContact() {
        Person person = Person.create(
                1L,
                "  " + "n".repeat(80) + "  ",
                null,
                null,
                "  " + "c".repeat(120) + "  "
        );
        Person blankContact = Person.create(1L, "Alice", null, null, "   ");

        assertThat(person.getDisplayName()).hasSize(80);
        assertThat(person.getContact()).hasSize(120);
        assertThat(blankContact.getContact()).isNull();
    }

    @Test
    void createsUnlinkedPersonAndFullyUpdatesProfile() {
        Person person = Person.create(1L, "  Alice  ", " note ", null, " contact ");
        LocalDate birthday = LocalDate.of(2000, 1, 2);

        person.update("Bob", "updated", birthday, null);

        assertThat(person.getOwnerPlayerId()).isEqualTo(1L);
        assertThat(person.getLinkedUserId()).isNull();
        assertThat(person.getDisplayName()).isEqualTo("Bob");
        assertThat(person.getNotes()).isEqualTo("updated");
        assertThat(person.getBirthday()).isEqualTo(birthday);
        assertThat(person.getContact()).isNull();
        assertThat(person.getStatus()).isEqualTo(PersonStatus.ACTIVE);
    }

    @Test
    void archiveIsIdempotentAndRejectsFurtherUpdates() {
        Person person = Person.create(1L, "Alice", null, null, null);

        person.archive();
        person.archive();

        assertThat(person.getStatus()).isEqualTo(PersonStatus.ARCHIVED);
        assertThatThrownBy(() -> person.update("Bob", null, null, null))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PersonError.PERSON_ARCHIVED)
                );
    }

    private void assertPersonError(Runnable action, PersonError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
