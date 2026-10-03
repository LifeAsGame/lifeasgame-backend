package online.lifeasgame.role.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtCurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {"spring.test.mockmvc.print=NONE", "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(RoleRelationPersonStatusIntegrationTest.IdentityConfig.class)
@DisplayName("관계 Person 상태 HTTP·MySQL 조회 계약")
class RoleRelationPersonStatusIntegrationTest {
    private static final long OWNER = 38101L;
    private static final long OTHER = 38102L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_role_relation_person_status")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("Person 보관은 여러 Role의 관계 이력을 보존하며 상태를 batch 조회하고 읽기는 DB를 바꾸지 않는다")
    void preservesHistoryAndReadsOwnedStatus() throws Exception {
        long role = create("/api/v1/roles", "{\"roleType\":\"SELF\",\"name\":\"Self\"}", OWNER);
        long secondRole = create("/api/v1/roles", "{\"roleType\":\"WORK\",\"name\":\"Work\"}", OWNER);
        long person = create("/api/v1/persons", "{\"displayName\":\"Alice\"}", OWNER);
        long activePerson = create("/api/v1/persons", "{\"displayName\":\"Bob\"}", OWNER);
        long foreignPerson = create("/api/v1/persons", "{\"displayName\":\"Private\"}", OTHER);
        long relation = createRelation(role, person);
        long secondRelation = createRelation(secondRole, person);
        createRelation(role, activePerson);
        var relationsBefore = jdbc.queryForList("SELECT * FROM role_relations ORDER BY id");

        mvc.perform(auth(delete("/api/v1/persons/" + person), OWNER)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForList("SELECT * FROM role_relations ORDER BY id")).isEqualTo(relationsBefore);
        var beforeReads = snapshot();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mvc.perform(auth(get(collection(role)).param("playerId", String.valueOf(OTHER)), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2))
                .andExpect(jsonPath("$.result[0].id").value(relation))
                .andExpect(jsonPath("$.result[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.result[0].personStatus").value("ARCHIVED"))
                .andExpect(jsonPath("$.result[0].personDisplayName").value("Alice"))
                .andExpect(jsonPath("$.result[0].roleNotes").value("note"))
                .andExpect(jsonPath("$.result[1].personStatus").value("ACTIVE"));
        assertThat(statistics.getPrepareStatementCount()).as("Role + relations + one Person batch").isEqualTo(3);
        for (var pair : Map.of(role, relation, secondRole, secondRelation).entrySet()) {
            mvc.perform(auth(get(collection(pair.getKey()) + "/" + pair.getValue()), OWNER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.result.personStatus").value("ARCHIVED"));
        }
        mvc.perform(get(collection(role))).andExpect(status().isUnauthorized());
        mvc.perform(auth(get(collection(role)), OTHER)).andExpect(status().isNotFound());
        mvc.perform(auth(get(collection(role) + "/" + relation), OTHER)).andExpect(status().isNotFound());
        mvc.perform(auth(post(collection(role)).contentType(MediaType.APPLICATION_JSON)
                        .content(relationBody(foreignPerson)), OWNER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PER-404-NOT-FOUND"));
        assertThat(snapshot()).isEqualTo(beforeReads);

        mvc.perform(auth(put(collection(role) + "/" + relation).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationType\":\"FAMILY\",\"roleNotes\":\"updated\"}"), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.personStatus").value("ARCHIVED"));
        mvc.perform(auth(delete(collection(role) + "/" + relation), OWNER)).andExpect(status().isNoContent());
        mvc.perform(auth(get(collection(role) + "/" + relation), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.result.personStatus").value("ARCHIVED"));
        mvc.perform(auth(get(collection(secondRole)), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(secondRelation))
                .andExpect(jsonPath("$.result[0].status").value("ACTIVE"));
    }

    private long createRelation(long roleId, long personId) throws Exception {
        var response = mvc.perform(auth(post(collection(roleId)).contentType(MediaType.APPLICATION_JSON)
                        .content(relationBody(personId)), OWNER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.personStatus").value("ACTIVE"))
                .andReturn().getResponse();
        return json.readTree(response.getContentAsString()).path("result").path("id").longValue();
    }

    @Test
    @DisplayName("실제 OpenAPI endpoint의 목록·상세·생성·수정 스키마에 personStatus가 포함된다")
    void documentsHttpResponses() throws Exception {
        var response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse();
        var spec = json.readTree(response.getContentAsString());
        String collection = "/api/v1/roles/{roleId}/relations";
        String detail = collection + "/{relationId}";
        for (var operation : List.of(List.of(collection, "get", "200"), List.of(detail, "get", "200"),
                List.of(collection, "post", "201"), List.of(detail, "put", "200"))) {
            var envelope = spec.path("paths").path(operation.get(0)).path(operation.get(1))
                    .path("responses").path(operation.get(2)).path("content").path("*/*").path("schema");
            if (envelope.isMissingNode()) {
                envelope = spec.path("paths").path(operation.get(0)).path(operation.get(1))
                        .path("responses").path(operation.get(2)).path("content").path("application/json").path("schema");
            }
            envelope = spec.at(envelope.path("$ref").asText().substring(1));
            var result = envelope.path("properties").path("result");
            if ("array".equals(result.path("type").asText())) result = result.path("items");
            var relation = spec.at(result.path("$ref").asText().substring(1));
            assertThat(relation.path("properties").path("personStatus").path("enum"))
                    .as("%s relation schema: %s", operation, relation).isEqualTo(json.readTree("[\"ACTIVE\",\"ARCHIVED\"]"));
            assertThat(relation.path("required")).contains(json.getNodeFactory().textNode("personStatus"));
        }
    }

    private long create(String path, String body, long owner) throws Exception {
        var response = mvc.perform(auth(post(path).contentType(MediaType.APPLICATION_JSON).content(body), owner))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).path("result").path("id").longValue();
    }
    private String collection(long role) { return "/api/v1/roles/" + role + "/relations"; }
    private String relationBody(long person) {
        return "{\"personId\":" + person + ",\"relationType\":\"FRIEND\",\"roleNotes\":\"note\"}";
    }
    private Map<String, List<Map<String, Object>>> snapshot() {
        var result = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String table : List.of("roles", "persons", "role_relations", "outbox_events")) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        }
        return result;
    }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, long player) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(new JwtPrincipal(player, player),
                null, List.of(new SimpleGrantedAuthority("ROLE_USER")))));
    }
    @TestConfiguration
    static class IdentityConfig {
        @Bean @Primary
        CurrentPlayerAccessor identity() { return new JwtCurrentPlayerAccessor(); }
    }
}
