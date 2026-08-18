package online.lifeasgame.social.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi.PlayerSummary;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.result.ChatResult;
import online.lifeasgame.social.domain.ChannelParticipant;
import online.lifeasgame.social.domain.ChannelRole;
import online.lifeasgame.social.domain.ChatChannel;
import online.lifeasgame.social.domain.error.SocialError;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
@DisplayName("Current Player Direct Friend Chat query")
class FriendChatQueryIntegrationTest {

    private static final Long CURRENT_PLAYER_ID = 288L;
    private static final Long FIRST_PEER_ID = 289L;
    private static final Long SECOND_PEER_ID = 290L;

    @Autowired
    private FriendChatQueryService friendChatQueryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @MockitoBean
    private PlayerConnectionReadApi playerConnectionReadApi;

    @BeforeEach
    void currentPlayer() {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(CURRENT_PLAYER_ID);
    }

    @Nested
    @DisplayName("친구 채널을 조회할 때")
    class FriendChannels {

        @Test
        @DisplayName("FRIEND만 channelId 순으로 반환하고 peer를 한 번에 조회한다")
        void mapsFriendsInStableOrderWithoutNPlusOne() {
            ChatChannel first = persist(ChatChannel.friend("First"), CURRENT_PLAYER_ID, FIRST_PEER_ID);
            persist(ChatChannel.global("Global"), CURRENT_PLAYER_ID);
            ChatChannel second = persist(ChatChannel.friend("Second"), CURRENT_PLAYER_ID, SECOND_PEER_ID);
            second.markReadOnly();
            flushAndClear();
            given(playerConnectionReadApi.findAllByPlayerIds(Set.of(FIRST_PEER_ID, SECOND_PEER_ID)))
                    .willReturn(Map.of(
                            FIRST_PEER_ID, new PlayerSummary(FIRST_PEER_ID, "First Peer", "MAGE", 7),
                            SECOND_PEER_ID, new PlayerSummary(SECOND_PEER_ID, "Second Peer", "WARRIOR", 8)
                    ));
            Statistics statistics = statistics();
            statistics.clear();

            List<ChatResult.FriendChannel> result = friendChatQueryService.friendChannels();

            assertThat(result).containsExactly(
                    new ChatResult.FriendChannel(
                            first.getId(),
                            new ChatResult.Peer(FIRST_PEER_ID, "First Peer", "MAGE", 7),
                            false
                    ),
                    new ChatResult.FriendChannel(
                            second.getId(),
                            new ChatResult.Peer(SECOND_PEER_ID, "Second Peer", "WARRIOR", 8),
                            true
                    )
            );
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
            verify(playerConnectionReadApi).findAllByPlayerIds(Set.of(FIRST_PEER_ID, SECOND_PEER_ID));
        }

        @Test
        @DisplayName("FRIEND가 없으면 participant와 Character batch를 조회하지 않는다")
        void returnsEmptyWithoutBatchQueries() {
            persist(ChatChannel.global("Global"), CURRENT_PLAYER_ID);
            flushAndClear();
            Statistics statistics = statistics();
            statistics.clear();

            assertThat(friendChatQueryService.friendChannels()).isEmpty();

            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
            verify(playerConnectionReadApi, never()).findAllByPlayerIds(anySet());
        }

        @Test
        @DisplayName("Character peer가 없으면 CHAT_PEER_NOT_FOUND로 거부한다")
        void rejectsMissingPeer() {
            persist(ChatChannel.friend("Missing"), CURRENT_PLAYER_ID, FIRST_PEER_ID);
            flushAndClear();
            given(playerConnectionReadApi.findAllByPlayerIds(Set.of(FIRST_PEER_ID)))
                    .willReturn(Map.of());

            assertChatError(SocialError.CHAT_PEER_NOT_FOUND);
        }

        @Test
        @DisplayName("peer participant가 정확히 한 명이 아니면 controlled error로 거부한다")
        void rejectsInvalidParticipantCardinality() {
            persist(
                    ChatChannel.friend("Malformed"),
                    CURRENT_PLAYER_ID,
                    FIRST_PEER_ID,
                    SECOND_PEER_ID
            );
            flushAndClear();

            assertChatError(SocialError.CHAT_FRIEND_PARTICIPANT_INVALID);

            verify(playerConnectionReadApi, never()).findAllByPlayerIds(anySet());
        }
    }

    private ChatChannel persist(ChatChannel channel, Long... playerIds) {
        entityManager.persist(channel);
        for (Long playerId : playerIds) {
            entityManager.persist(new ChannelParticipant(channel, playerId, ChannelRole.MEMBER));
        }
        return channel;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private void assertChatError(SocialError error) {
        assertThatThrownBy(friendChatQueryService::friendChannels)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
