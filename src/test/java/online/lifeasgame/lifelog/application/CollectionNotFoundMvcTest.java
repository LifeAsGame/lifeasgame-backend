package online.lifeasgame.lifelog.application;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.api.admin.AdminCollectionController;
import online.lifeasgame.lifelog.api.player.PlayerCollectionController;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.lifelog.domain.*;
import online.lifeasgame.lifelog.domain.repository.CollectionLogRepository;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PlayerCollectionController.class, AdminCollectionController.class})
@Import({SecurityConfig.class, WebMvcTestConfig.class, CollectionLogReader.class,
        CollectionLogQueryService.class, CollectionLogService.class})
@DisplayName("Collection 상세·수정 HTTP 계약")
class CollectionNotFoundMvcTest {

    private static final long PLAYER = 25601L;
    private static final long OTHER_PLAYER = 25602L;
    private static final String PLAYER_PATH = "/api/v1/players/collections";
    private static final String ADMIN_PATH = "/admin/v1/players/";

    @Autowired MockMvc mockMvc;
    @MockitoBean CollectionLogRepository repository;
    @MockitoBean CollectionLogWriter writer;
    @MockitoBean LifeLogRecordRegistrar registrar;
    @MockitoBean DomainEventPublisher publisher;
    @MockitoBean CurrentPlayerAccessor currentPlayerAccessor;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean AppErrorProperties appErrorProperties;
    @MockitoBean ErrorDocLinker errorDocLinker;

    private CollectionLog otherCollection;

    @BeforeEach
    void records() {
        CollectionLog ownCollection = collection(11L, PLAYER, 2);
        otherCollection = collection(22L, OTHER_PLAYER, 3);
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER);
        given(repository.findByIdAndPlayerId(11L, PLAYER)).willReturn(Optional.of(ownCollection));
        given(repository.findByIdAndPlayerId(22L, OTHER_PLAYER)).willReturn(Optional.of(otherCollection));
    }

    @Test
    @DisplayName("본인 상세·수정은 기존 응답 형태로 성공한다")
    void ownDetailAndUpdate() throws Exception {
        mockMvc.perform(authenticated(get(PLAYER_PATH + "/11"), "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(11))
                .andExpect(jsonPath("$.result.quantity").value(2));

        mockMvc.perform(authenticated(post(PLAYER_PATH + "/11"), "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.quantity").value(4));
        verify(repository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("미존재·타인 소유 상세·수정은 동일한 404이며 타인 기록은 불변이다")
    void hiddenAndMissing() throws Exception {
        for (long id : new long[]{22L, 99L}) {
            mockMvc.perform(authenticated(get(PLAYER_PATH + "/" + id), "ROLE_USER"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("LIF-404-COLLECTION-NOT-FOUND"))
                    .andExpect(jsonPath("$.title").value("Collection Not Found"));
            mockMvc.perform(authenticated(post(PLAYER_PATH + "/" + id), "ROLE_USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":9}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("LIF-404-COLLECTION-NOT-FOUND"))
                    .andExpect(jsonPath("$.title").value("Collection Not Found"));
        }
        assertThat(otherCollection.getQuantity().value()).isEqualTo(3);
        verify(repository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("관리자 경로도 지정 플레이어 조건으로 조회하고 관리자 권한을 요구한다")
    void adminScopeAndAccess() throws Exception {
        String correct = ADMIN_PATH + OTHER_PLAYER + "/collections/22";
        String wrong = ADMIN_PATH + PLAYER + "/collections/22";
        mockMvc.perform(authenticated(get(correct), "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.playerId").value(OTHER_PLAYER));
        mockMvc.perform(authenticated(post(correct), "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.quantity").value(5));
        mockMvc.perform(authenticated(get(wrong), "ROLE_ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIF-404-COLLECTION-NOT-FOUND"));
        mockMvc.perform(authenticated(post(wrong), "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":9}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIF-404-COLLECTION-NOT-FOUND"));
        assertThat(otherCollection.getQuantity().value()).isEqualTo(5);
        mockMvc.perform(get(correct)).andExpect(status().isUnauthorized());
        mockMvc.perform(authenticated(get(correct), "ROLE_USER"))
                .andExpect(status().isForbidden());
        verify(repository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("잘못된 수정 입력은 400이고 삭제 응답은 반복 호출해도 유지된다")
    void validationAndDelete() throws Exception {
        mockMvc.perform(authenticated(post(PLAYER_PATH + "/11"), "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQ-VALIDATION"));
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(authenticated(delete(PLAYER_PATH + "/99"), "ROLE_USER"))
                    .andExpect(status().isNoContent());
        }
        verify(writer, times(2)).delete(PLAYER, 99L);
    }

    private CollectionLog collection(Long id, Long playerId, int quantity) {
        CollectionLog log = CollectionLog.create(playerId, CollectionCategory.BOOK,
                Title.of("Book", null), Quantity.of(quantity), null, null, CollectionTags.of(null));
        ReflectionTestUtils.setField(log, "id", id);
        return log;
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, String role) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(
                        new JwtPrincipal(256L, PLAYER), null,
                        List.of(new SimpleGrantedAuthority(role)))))
                .header("Authorization", "Bearer test-token");
    }
}
