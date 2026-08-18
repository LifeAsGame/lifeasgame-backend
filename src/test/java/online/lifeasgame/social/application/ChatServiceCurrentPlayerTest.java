package online.lifeasgame.social.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.application.result.ChatResult;
import online.lifeasgame.social.domain.ChannelParticipant;
import online.lifeasgame.social.domain.ChatChannel;
import online.lifeasgame.social.domain.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Current Player ownership")
class ChatServiceCurrentPlayerTest {

    private static final Long PLAYER_ID = 290L;

    @Mock
    private ChatReader chatReader;

    @Mock
    private ChatWriter chatWriter;

    @Mock
    private GuildReader guildReader;

    @Mock
    private PartyReader partyReader;

    @Mock
    private FriendshipVerifier friendshipVerifier;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @InjectMocks
    private ChatService chatService;

    @Nested
    @DisplayName("player-facing use case를 호출할 때")
    class PlayerFacingUseCase {

        @Test
        @DisplayName("messages는 Current Player로 membership을 조회한다")
        void readsMessagesAsCurrentPlayer() {
            ChatResult.MessagePage expected = ChatResult.MessagePage.from(List.of(), false, null);
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
            given(chatReader.messages(291L, PLAYER_ID, 292L, 20)).willReturn(expected);

            ChatResult.MessagePage result = chatService.messages(291L, 292L, 20);

            assertThat(result).isSameAs(expected);
            verify(currentPlayerAccessor, times(1)).currentPlayerIdOrThrow();
            verify(chatReader).messages(291L, PLAYER_ID, 292L, 20);
        }

        @Test
        @DisplayName("sendMessage는 Current Player를 sender로 사용한다")
        void sendsMessageAsCurrentPlayer() {
            ChatChannel channel = mock(ChatChannel.class);
            ChatMessage message = mock(ChatMessage.class);
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
            given(chatReader.getMemberChannel(291L, PLAYER_ID)).willReturn(channel);
            given(chatWriter.publish(channel, PLAYER_ID, "hello")).willReturn(message);
            given(message.getChannel()).willReturn(channel);
            given(channel.getId()).willReturn(291L);

            chatService.sendMessage(291L, new ChatCommand.SendMessage("hello"));

            verify(currentPlayerAccessor, times(1)).currentPlayerIdOrThrow();
            verify(chatReader).getMemberChannel(291L, PLAYER_ID);
            verify(chatWriter).publish(channel, PLAYER_ID, "hello");
        }

        @Test
        @DisplayName("openFriend는 resolved Player로 friendship을 검증한다")
        void verifiesFriendshipAsCurrentPlayer() {
            ChatChannel channel = mock(ChatChannel.class);
            ChannelParticipant participant = mock(ChannelParticipant.class);
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
            given(chatWriter.ensureFriendChannel(PLAYER_ID, 291L, "친구 채팅"))
                    .willReturn(channel);
            given(chatWriter.join(channel, PLAYER_ID, null)).willReturn(participant);
            given(participant.getChannel()).willReturn(channel);

            chatService.openFriend(291L, new ChatCommand.OpenFriend(null));

            verify(currentPlayerAccessor, times(1)).currentPlayerIdOrThrow();
            verify(friendshipVerifier).verify(PLAYER_ID, 291L);
        }
    }
}
