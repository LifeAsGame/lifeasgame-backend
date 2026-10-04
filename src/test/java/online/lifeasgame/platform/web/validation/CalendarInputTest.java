package online.lifeasgame.platform.web.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import online.lifeasgame.character.api.player.request.PlayerCertificationRequest;
import online.lifeasgame.person.api.request.PersonRequest;
import online.lifeasgame.role.api.request.RoleEventRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사용자 입력 달력 날짜 검증")
class CalendarInputTest {
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("DATE 컬럼은 실제 날짜와 1000~9999년만 받아들이고 null은 유지한다")
    void dateColumns() throws Exception {
        for (String date : new String[]{"1000-01-01", "2024-02-29", "9999-12-31"}) {
            var request = json.readValue("{\"acquiredDate\":\"" + date + "\"}", PlayerCertificationRequest.Create.class);
            assertThat(validator.validate(request)).isEmpty();
        }
        assertThat(validator.validate(new PlayerCertificationRequest.Create(null, null))).isEmpty();
        for (String date : new String[]{"0000-01-01", "-0001-01-01", "0001-01-01", "0999-12-31", "+10000-01-01", "+100000-01-01"}) {
            var request = json.readValue("{\"acquiredDate\":\"" + date + "\"}", PlayerCertificationRequest.Create.class);
            assertThat(validator.validate(request)).as(date).isNotEmpty();
        }
        assertThatThrownBy(() -> json.readValue("{\"acquiredDate\":\"2025-02-29\"}", PlayerCertificationRequest.Create.class))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Person profile JSON 날짜는 0001년부터 허용하고 확장 연도를 거부한다")
    void profileDates() throws Exception {
        var valid = json.readValue("{\"displayName\":\"A\",\"profile\":{\"ageReferenceDate\":\"0001-01-01\",\"importantDates\":[{\"label\":\"day\",\"date\":\"9999-12-31\",\"repeatYearly\":true}]}}", PersonRequest.Create.class);
        assertThat(validator.validate(valid)).isEmpty();
        var invalid = json.readValue("{\"displayName\":\"A\",\"profile\":{\"firstMetOn\":\"+100000-01-01\"}}", PersonRequest.Create.class);
        assertThat(validator.validate(invalid)).isNotEmpty();
        var update = json.readValue("{\"profile\":{\"importantDates\":[{\"label\":\"day\",\"date\":\"0000-01-01\",\"repeatYearly\":true}]}}", PersonRequest.Update.class);
        assertThat(validator.validate(update)).isNotEmpty();
    }

    @Test
    @DisplayName("일정은 유효한 offset과 서울 DB 저장 범위를 요구하며 미래 날짜를 허용한다")
    void scheduleInstants() throws Exception {
        for (String date : new String[]{"1000-01-01T00:00:00Z", "9999-12-31T14:59:59Z", "2030-02-28T12:00:00+09:00"}) {
            assertThat(json.readValue("{\"title\":\"A\",\"startsAt\":\"" + date + "\"}", RoleEventRequest.Create.class).startsAt()).isNotNull();
        }
        for (String date : new String[]{"0000-01-01T00:00:00Z", "-0001-01-01T00:00:00Z", "0999-12-31T00:00:00Z", "+10000-01-01T00:00:00Z", "+100000-01-01T00:00:00Z", "2025-02-29T12:00:00Z", "2026-01-01T12:00:00", "9999-12-31T15:00:00Z"}) {
            assertThatThrownBy(() -> json.readValue("{\"title\":\"A\",\"startsAt\":\"" + date + "\"}", RoleEventRequest.Create.class)).as(date).isInstanceOf(Exception.class);
        }
    }
}
