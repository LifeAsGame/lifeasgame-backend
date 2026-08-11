package online.lifeasgame.user.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.HashedPassword;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserStatus;
import online.lifeasgame.user.domain.error.UserError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLookupApi provider")
class UserLookupServiceTest {

    @Mock
    private UserReader reader;

    @InjectMocks
    private UserLookupService service;

    @Nested
    @DisplayName("SERVICE_USER reference를 조회할 때")
    class GetActive {

        @Test
        @DisplayName("ACTIVE User의 provider-owned ID와 nickname만 반환한다")
        void returnsActiveReference() {
            User user = user();
            given(reader.findByIdOrElseThrow(7L)).willReturn(user);

            var reference = service.getActive(7L);

            assertThat(reference.id()).isEqualTo(7L);
            assertThat(reference.nickname()).isEqualTo("service-user");
        }

        @Test
        @DisplayName("PENDING/BANNED/DELETED User는 신규 참여자로 제공하지 않는다")
        void rejectsInactiveStatuses() {
            for (UserStatus status : new UserStatus[]{
                    UserStatus.PENDING_EMAIL_VERIFICATION,
                    UserStatus.BANNED,
                    UserStatus.DELETED
            }) {
                User user = user();
                ReflectionTestUtils.setField(user, "status", status);
                given(reader.findByIdOrElseThrow(7L)).willReturn(user);

                assertThatThrownBy(() -> service.getActive(7L))
                        .isInstanceOfSatisfying(
                                DomainException.class,
                                exception -> assertThat(
                                        exception.getErrorCode()
                                ).isEqualTo(UserError.USER_NOT_ACTIVE)
                        );
            }
        }
    }

    private User user() {
        User user = User.register(
                Email.of("service@example.com"),
                HashedPassword.of("h".repeat(20)),
                Nickname.of("service-user")
        );
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
}
