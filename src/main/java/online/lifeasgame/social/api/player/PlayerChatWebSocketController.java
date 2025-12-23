package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.api.player.mapper.PlayerChatWebMapper;
import online.lifeasgame.social.api.player.request.PlayerChatSocketRequest;
import online.lifeasgame.social.application.ChatFacade;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
@RequiredArgsConstructor
public class PlayerChatWebSocketController {

    private final ChatFacade chatFacade;

    @MessageMapping("/social/chat/{channelId}/send")
    public void publish(
            @DestinationVariable Long channelId,
            @Valid PlayerChatSocketRequest.SendMessage request
    ) {
        chatFacade.sendMessage(channelId, PlayerChatWebMapper.toSendMessageCommand(request));
    }
}
