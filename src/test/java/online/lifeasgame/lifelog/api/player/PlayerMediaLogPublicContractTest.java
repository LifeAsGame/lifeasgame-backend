package online.lifeasgame.lifelog.api.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.lifelog.api.player.mapper.PlayerMediaLogWebMapper;
import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Player MediaLog public command contract")
class PlayerMediaLogPublicContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Rate Advance MarkStatus DTO는 discarded idempotencyKey를 노출하지 않는다")
    void exposesOnlySupportedFields() {
        assertThat(componentNames(PlayerMediaLogRequest.Rate.class))
                .containsExactly("score");
        assertThat(componentNames(PlayerMediaLogRequest.Advance.class))
                .containsExactly("step");
        assertThat(componentNames(PlayerMediaLogRequest.MarkStatus.class))
                .containsExactly("status");
    }

    @Test
    @DisplayName("지원 payload를 기존 Media application command로 그대로 매핑한다")
    void preservesCommands() throws Exception {
        assertThat(PlayerMediaLogWebMapper.toRateCommand(
                objectMapper.readValue(
                        "{\"score\":4.5}",
                        PlayerMediaLogRequest.Rate.class
                )
        )).isEqualTo(new MediaLogCommand.Rate(4.5));
        assertThat(PlayerMediaLogWebMapper.toAdvanceCommand(
                objectMapper.readValue(
                        "{\"step\":2}",
                        PlayerMediaLogRequest.Advance.class
                )
        )).isEqualTo(new MediaLogCommand.Advance(2));
        assertThat(PlayerMediaLogWebMapper.toMarkStatusCommand(
                objectMapper.readValue(
                        "{\"status\":\"WATCHING\"}",
                        PlayerMediaLogRequest.MarkStatus.class
                )
        )).isEqualTo(new MediaLogCommand.MarkStatus("WATCHING"));
    }

    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .toList();
    }
}
