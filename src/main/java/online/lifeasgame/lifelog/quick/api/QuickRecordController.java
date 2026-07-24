package online.lifeasgame.lifelog.quick.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.quick.application.QuickRecordFacade;
import online.lifeasgame.lifelog.quick.application.QuickRecordResult;
import online.lifeasgame.lifelog.quick.domain.error.QuickRecordError;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lifelogs")
public class QuickRecordController implements QuickRecordSpecV1 {

    private final QuickRecordFacade quickRecordFacade;

    @Override
    @PostMapping("/quick-record")
    public ResponseEntity<ApiResponse<QuickRecordResponse.Recorded>> record(
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey,
            @Valid @RequestBody QuickRecordRequest.Create request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new DomainException(
                    QuickRecordError.IDEMPOTENCY_KEY_REQUIRED
            );
        }
        QuickRecordResult.Recorded result = quickRecordFacade.record(
                idempotencyKey,
                QuickRecordWebMapper.toCommand(request)
        );
        QuickRecordResponse.Recorded response =
                QuickRecordWebMapper.toResponse(result);

        if (result.replay()) {
            return ApiResponses.ok(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onCreateSuccess(response));
    }
}
