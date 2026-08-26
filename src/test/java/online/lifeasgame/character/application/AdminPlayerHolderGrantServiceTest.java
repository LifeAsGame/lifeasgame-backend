package online.lifeasgame.character.application;

import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.character.application.command.AdminPlayerHolderGrantCommand;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Player holder grant application contract")
class AdminPlayerHolderGrantServiceTest {

    private static final long PLAYER_ID = 316L;
    private static final long DEFINITION_ID = 3160L;

    @Mock
    private PlayerAchievementService playerAchievementService;

    @Mock
    private PlayerTitleService playerTitleService;

    @Mock
    private AdminAuditInternalApi adminAuditApi;

    @Nested
    @DisplayName("Admin holder grant metadata를 만들 때")
    class CommandValidation {

        @Test
        @DisplayName("positive identity와 safe reason/header만 허용한다")
        void rejectsUnsafeMetadata() {
            assertThatThrownBy(() -> achievement(
                    0L, DEFINITION_ID, "CASE-316", "key-316", null
            )).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> title(
                    PLAYER_ID, -1L, "CASE-316", "key-316", null
            )).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> achievement(
                    PLAYER_ID,
                    DEFINITION_ID,
                    "CASE-316\u202Eprivate",
                    "key-316",
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single-line");
            assertThatThrownBy(() -> title(
                    PLAYER_ID,
                    DEFINITION_ID,
                    "CASE-316",
                    "unsafe key",
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
            assertThatThrownBy(() -> achievement(
                    PLAYER_ID,
                    DEFINITION_ID,
                    "CASE-316",
                    "key-316",
                    "unsafe correlation"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
        }

        @Test
        @DisplayName("correlation이 없으면 safe UUID를 만들고 reason을 정규화한다")
        void generatesCorrelation() {
            assertThat(achievement(
                    PLAYER_ID,
                    DEFINITION_ID,
                    " CASE-316 ",
                    "key-316",
                    null
            )).satisfies(command -> {
                assertThat(command.reason()).isEqualTo("CASE-316");
                assertThat(command.correlationId()).matches("[a-f0-9-]{36}");
            });
        }
    }

    @Test
    @DisplayName("Achievement holder를 생성한 뒤 relation identity로 success Audit을 append한다")
    void grantsAchievementThenAudits() {
        var expected = new PlayerAchievementResult.Granted(
                PLAYER_ID,
                DEFINITION_ID,
                "ACH-316",
                "Achievement 316",
                "STORY",
                Instant.parse("2026-08-26T00:00:00Z")
        );
        when(playerAchievementService.grantAchievement(
                PLAYER_ID,
                DEFINITION_ID
        )).thenReturn(expected);
        AdminPlayerHolderGrantService service = service();

        var result = service.grantAchievement(achievement(
                PLAYER_ID,
                DEFINITION_ID,
                "CASE-316-ACHIEVEMENT",
                "achievement-316",
                "request-achievement"
        ));

        assertThat(result).isSameAs(expected);
        assertGrantAndAudit(
                inOrder(playerAchievementService, adminAuditApi),
                true,
                "PLAYER_ACHIEVEMENT_GRANT",
                "PLAYER_ACHIEVEMENT",
                "CASE-316-ACHIEVEMENT",
                "achievement-316",
                "request-achievement"
        );
    }

    @Test
    @DisplayName("Title holder를 생성한 뒤 relation identity로 success Audit을 append한다")
    void grantsTitleThenAudits() {
        var expected = new PlayerTitleResult.Created(
                PLAYER_ID,
                DEFINITION_ID,
                "TITLE-316",
                "Title 316",
                "SPECIAL",
                Instant.parse("2026-08-26T00:00:00Z")
        );
        when(playerTitleService.createTitle(
                PLAYER_ID,
                DEFINITION_ID
        )).thenReturn(expected);
        AdminPlayerHolderGrantService service = service();

        var result = service.grantTitle(title(
                PLAYER_ID,
                DEFINITION_ID,
                "CASE-316-TITLE",
                "title-316",
                "request-title"
        ));

        assertThat(result).isSameAs(expected);
        assertGrantAndAudit(
                inOrder(playerTitleService, adminAuditApi),
                false,
                "PLAYER_TITLE_GRANT",
                "PLAYER_TITLE",
                "CASE-316-TITLE",
                "title-316",
                "request-title"
        );
    }

    @Test
    @DisplayName("holder mutation이 실패하면 success Audit을 시도하지 않는다")
    void doesNotAuditFailedMutation() {
        when(playerAchievementService.grantAchievement(
                PLAYER_ID,
                DEFINITION_ID
        )).thenThrow(new IllegalStateException("holder failure"));
        AdminPlayerHolderGrantService service = service();

        assertThatThrownBy(() -> service.grantAchievement(achievement(
                PLAYER_ID,
                DEFINITION_ID,
                "CASE-316",
                "key-316",
                null
        ))).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(adminAuditApi);
    }

    private void assertGrantAndAudit(
            InOrder order,
            boolean achievement,
            String action,
            String targetType,
            String reason,
            String key,
            String correlationId
    ) {
        if (achievement) {
            order.verify(playerAchievementService).grantAchievement(
                    PLAYER_ID,
                    DEFINITION_ID
            );
        } else {
            order.verify(playerTitleService).createTitle(
                    PLAYER_ID,
                    DEFINITION_ID
            );
        }
        ArgumentCaptor<AdminAuditInternalApi.AppendCommand> audit =
                ArgumentCaptor.forClass(AdminAuditInternalApi.AppendCommand.class);
        order.verify(adminAuditApi).append(audit.capture());
        assertThat(audit.getValue()).satisfies(command -> {
            assertThat(command.action().value()).isEqualTo(action);
            assertThat(command.targetType().value()).isEqualTo(targetType);
            assertThat(command.targetId()).isEqualTo("316:3160");
            assertThat(command.reason()).isEqualTo(reason);
            assertThat(command.result()).isEqualTo(AdminAuditResult.SUCCESS);
            assertThat(command.idempotencyKey()).isEqualTo(key);
            assertThat(command.correlationId()).isEqualTo(correlationId);
        });
    }

    private AdminPlayerHolderGrantService service() {
        return new AdminPlayerHolderGrantService(
                playerAchievementService,
                playerTitleService,
                adminAuditApi
        );
    }

    private AdminPlayerHolderGrantCommand.GrantAchievement achievement(
            Long playerId,
            Long achievementId,
            String reason,
            String key,
            String correlationId
    ) {
        return new AdminPlayerHolderGrantCommand.GrantAchievement(
                playerId,
                achievementId,
                reason,
                key,
                correlationId
        );
    }

    private AdminPlayerHolderGrantCommand.GrantTitle title(
            Long playerId,
            Long titleId,
            String reason,
            String key,
            String correlationId
    ) {
        return new AdminPlayerHolderGrantCommand.GrantTitle(
                playerId,
                titleId,
                reason,
                key,
                correlationId
        );
    }
}
