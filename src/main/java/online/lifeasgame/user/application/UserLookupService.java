package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.user.application.internal.UserLookupApi;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserStatus;
import online.lifeasgame.user.domain.error.UserError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class UserLookupService implements UserLookupApi {

    private final UserReader reader;

    @Override
    public UserReference getActive(Long userId) {
        User user = reader.findByIdOrElseThrow(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException(UserError.USER_NOT_ACTIVE);
        }
        return new UserReference(
                user.getId(),
                user.getNickname().getValue()
        );
    }
}
