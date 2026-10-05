package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.application.internal.PersonLinkApi;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.PersonStatus;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.person.domain.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonLinkService implements PersonLinkApi {
    private final PersonRepository persons;
    private final PlayerLookupApi players;

    @Override
    @Transactional(readOnly = true)
    public Link find(Long ownerPlayerId, Long userId) {
        return persons.findByOwnerPlayerIdAndLinkedUserId(ownerPlayerId, userId)
                .map(PersonLinkService::link).orElseGet(Link::absent);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Link select(Long ownerPlayerId, Long userId, Long personId) {
        players.lockPlayer(ownerPlayerId);
        Person selected = persons.findByIdAndOwnerPlayerId(personId, ownerPlayerId)
                .orElseThrow(() -> new DomainException(PersonError.PERSON_NOT_FOUND));
        if (selected.getStatus() == PersonStatus.ARCHIVED) throw new DomainException(PersonError.PERSON_ARCHIVED);
        if (selected.getLinkedUserId() != null && !selected.getLinkedUserId().equals(userId))
            throw new DomainException(PersonError.PERSON_LINK_CONFLICT);
        Person existing = persons.findByOwnerPlayerIdAndLinkedUserId(ownerPlayerId, userId).orElse(null);
        if (existing != null && !existing.getId().equals(personId))
            throw new DomainException(PersonError.PERSON_LINK_CONFLICT);
        selected.linkUser(userId);
        return link(persons.save(selected));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Link create(Long ownerPlayerId, Long userId, PersonCommand.Create command) {
        players.lockPlayer(ownerPlayerId);
        Person existing = persons.findByOwnerPlayerIdAndLinkedUserId(ownerPlayerId, userId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == PersonStatus.ARCHIVED) throw new DomainException(PersonError.PERSON_ARCHIVED);
            return link(existing);
        }
        Person created = Person.create(ownerPlayerId, command.displayName(), command.notes(),
                command.birthday(), command.contact());
        created.replaceProfile(command.profile());
        created.linkUser(userId);
        return link(persons.save(created));
    }

    @Override
    @Transactional
    public void unlink(Long ownerPlayerId, Long personId) {
        players.lockPlayer(ownerPlayerId);
        Person person = persons.findByIdAndOwnerPlayerId(personId, ownerPlayerId)
                .orElseThrow(() -> new DomainException(PersonError.PERSON_NOT_FOUND));
        person.unlinkUser();
        persons.save(person);
    }

    private static Link link(Person person) { return new Link(person.getId(), person.getStatus().name()); }
}
