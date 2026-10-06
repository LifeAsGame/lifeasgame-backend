package online.lifeasgame.person.infra;

import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.PersonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JpaPersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByIdAndOwnerPlayerId(Long id, Long ownerPlayerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Person p WHERE p.id = :id AND p.ownerPlayerId = :owner")
    Optional<Person> findOwnedForUpdate(@Param("id") Long id, @Param("owner") Long ownerPlayerId);

    Optional<Person> findByOwnerPlayerIdAndLinkedUserId(Long ownerPlayerId, Long linkedUserId);

    List<Person> findAllByIdInAndOwnerPlayerId(
            Set<Long> ids,
            Long ownerPlayerId
    );

    List<Person> findAllByOwnerPlayerIdAndStatusOrderByIdAsc(
            Long ownerPlayerId,
            PersonStatus status
    );
}
