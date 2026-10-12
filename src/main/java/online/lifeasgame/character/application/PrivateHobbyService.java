package online.lifeasgame.character.application;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PersonalCategory.Kind;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
import online.lifeasgame.character.domain.error.PersonalCategoryError;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import online.lifeasgame.character.domain.repository.PersonalCategoryRepository;
import online.lifeasgame.character.domain.repository.PlayerHobbyRepository;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivateHobbyService {
    private final PlayerHobbyRepository hobbies;
    private final PersonalCategoryRepository categories;
    private final CurrentPlayerAccessor currentPlayer;

    public record Info(Long ownedItemId, Long catalogItemId, String source, String name, String detail,
                       int proficiency, String status, LocalDate startedOn, Long personalCategoryId) {
        static Info from(PlayerHobby hobby) {
            return new Info(hobby.getId(), null, "PRIVATE", hobby.getCustomName(), hobby.getDetail(),
                    hobby.getProficiency(), hobby.getStatus().name(), hobby.getStartedOn(), hobby.getPersonalCategoryId());
        }
    }

    @Transactional
    public Info create(String name, String detail, Integer proficiency, String status, LocalDate startedOn, Long categoryId) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        validateCategory(owner, categoryId);
        PlayerHobby hobby = PlayerHobby.createPrivate(owner, name, detail, proficiency == null ? 0 : proficiency,
                status == null ? PlayerHobbyStatus.ACTIVE : PlayerHobbyStatus.parse(status), startedOn);
        if (hobbies.existsPrivateName(owner, hobby.getPrivateNameKey())) {
            throw new DomainException(PlayerHobbyError.DUPLICATE_PRIVATE_HOBBY);
        }
        hobby.assignPersonalCategory(categoryId);
        try {
            return Info.from(hobbies.saveAndFlush(hobby));
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(PlayerHobbyError.DUPLICATE_PRIVATE_HOBBY);
        }
    }

    @Transactional(readOnly = true)
    public List<Info> list() {
        return hobbies.findPrivateByPlayerId(currentPlayer.currentPlayerIdOrThrow()).stream().map(Info::from).toList();
    }

    @Transactional(readOnly = true)
    public Info get(Long ownedItemId) {
        return Info.from(owned(ownedItemId));
    }

    @Transactional
    public Info update(Long ownedItemId, String name, String detail, Integer proficiency, String status,
                       LocalDate startedOn, Long categoryId, boolean categoryProvided) {
        PlayerHobby hobby = owned(ownedItemId);
        if (categoryProvided) validateCategory(hobby.getPlayerId(), categoryId);
        hobby.changePrivate(name == null ? hobby.getCustomName() : name, detail == null ? hobby.getDetail() : detail,
                proficiency == null ? hobby.getProficiency() : proficiency,
                status == null ? hobby.getStatus() : PlayerHobbyStatus.parse(status),
                startedOn == null ? hobby.getStartedOn() : startedOn);
        if (categoryProvided) hobby.assignPersonalCategory(categoryId);
        try {
            hobbies.saveAndFlush(hobby);
            return Info.from(hobby);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(PlayerHobbyError.DUPLICATE_PRIVATE_HOBBY);
        }
    }

    @Transactional
    public void delete(Long ownedItemId) {
        hobbies.delete(owned(ownedItemId));
    }

    private PlayerHobby owned(Long ownedItemId) {
        return hobbies.findPrivate(currentPlayer.currentPlayerIdOrThrow(), ownedItemId)
                .orElseThrow(() -> new DomainException(PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND));
    }

    private void validateCategory(Long owner, Long categoryId) {
        if (categoryId != null && !categories.existsByIdAndOwnerPlayerIdAndKind(categoryId, owner, Kind.HOBBY)) {
            throw new DomainException(PersonalCategoryError.NOT_FOUND);
        }
    }
}
