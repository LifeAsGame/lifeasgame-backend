package online.lifeasgame.person.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.error.PersonError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PersonLookupServiceTest {

    @Mock
    private PersonReader reader;

    @InjectMocks
    private PersonLookupService service;

    @Test
    void returnsProviderOwnedReferenceForOwnedActivePerson() {
        Person person = Person.create(1L, "Alice", null, null, null);
        ReflectionTestUtils.setField(person, "id", 2L);
        given(reader.getOwned(2L, 1L)).willReturn(person);

        var reference = service.getOwnedActive(2L, 1L);

        assertThat(reference.displayName()).isEqualTo("Alice");
        assertThat(reference.linkedUserId()).isNull();
    }

    @Test
    void preservesCrossOwnerNotFound() {
        given(reader.getOwned(2L, 1L)).willThrow(
                new DomainException(PersonError.PERSON_NOT_FOUND)
        );

        assertError(
                () -> service.getOwnedActive(2L, 1L),
                PersonError.PERSON_NOT_FOUND
        );
    }

    @Test
    void rejectsArchivedPerson() {
        Person person = Person.create(1L, "Alice", null, null, null);
        person.archive();
        given(reader.getOwned(2L, 1L)).willReturn(person);

        assertError(
                () -> service.getOwnedActive(2L, 1L),
                PersonError.PERSON_ARCHIVED
        );
    }

    @Test
    void returnsOwnedBatchIncludingArchivedReferences() {
        Person active = Person.create(1L, "Alice", null, null, null);
        Person archived = Person.create(1L, "Bob", null, null, null);
        ReflectionTestUtils.setField(active, "id", 2L);
        ReflectionTestUtils.setField(archived, "id", 3L);
        archived.archive();
        given(reader.findOwnedByIds(Set.of(2L, 3L), 1L))
                .willReturn(List.of(active, archived));

        var references = service.findOwnedByIds(Set.of(2L, 3L), 1L);

        assertThat(references.values())
                .extracting(reference -> reference.displayName())
                .containsExactlyInAnyOrder("Alice", "Bob");
    }

    private void assertError(Runnable action, PersonError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
