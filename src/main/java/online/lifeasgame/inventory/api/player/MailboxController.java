package online.lifeasgame.inventory.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.MailboxFacade;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.api.player.mapper.MailboxWebMapper;
import online.lifeasgame.inventory.api.player.request.MailboxRequest;
import online.lifeasgame.inventory.api.player.response.MailboxResponse;
import online.lifeasgame.inventory.api.player.spec.MailboxApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mailbox")
public class MailboxController implements MailboxApiSpecV1 {

    private final MailboxFacade mailboxFacade;

    @Override
    @PostMapping("/deliver")
    public ResponseEntity<ApiResponse<MailboxResponse.Slot>> deliver(
            @Valid @RequestBody MailboxRequest.Deliver request
    ) {
        MailboxResult.Slot slot = mailboxFacade.deliver(MailboxWebMapper.toDeliverCommand(request));
        return ApiResponses.ok(MailboxWebMapper.toSlot(slot));
    }

    @Override
    @PostMapping("/claim")
    public ResponseEntity<ApiResponse<Void>> claim(@Valid @RequestBody MailboxRequest.Claim request) {
        mailboxFacade.claim(MailboxWebMapper.toClaimCommand(request));
        return ApiResponses.noContent();
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<MailboxResponse.Mails>> list() {
        MailboxResult.Mails mails = mailboxFacade.list();
        return ApiResponses.ok(MailboxWebMapper.toLMails(mails));
    }
}
