package online.lifeasgame.person.infra;

import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.PersonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaPersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByIdAndOwnerPlayerId(Long id, Long ownerPlayerId);

    List<Person> findAllByOwnerPlayerIdAndStatusOrderByIdAsc(
            Long ownerPlayerId,
            PersonStatus status
    );
}
