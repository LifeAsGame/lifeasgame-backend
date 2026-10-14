package online.lifeasgame.character.api.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Player equipment OpenAPI")
class PlayerEquipmentOpenApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    @DisplayName("장착 해제는 본문 없는 204만 문서화한다")
    void documentsUnequipNoContent() throws Exception {
        var response = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse();
        var responses = json.readTree(response.getContentAsString())
                .path("paths").path("/api/v1/players/equipment/{slotId}")
                .path("delete").path("responses");

        assertThat(responses.has("204")).isTrue();
        assertThat(responses.path("204").has("content")).isFalse();
        assertThat(responses.has("200")).isFalse();
    }
}
