package online.lifeasgame.quest.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Admin Quest API V1")
public interface AdminQuestSpecV1 {

    @Operation(
            summary = "Quest Blueprint 목록",
            description = "서버에 정의된 Blueprint 목록입니다. semanticCategory/progressSource/repeatPolicy는 final 계약에만 존재하며, legacy repeatRule은 호환을 위해 유지됩니다."
    )
    ResponseEntity<AdminQuestResponse.Blueprints> catalog();

    @Operation(
            summary = "Quest Definition 목록",
            description = "DB Definition 목록입니다. repeatPolicy는 ONCE/DAILY/WEEKLY final 계약이고 repeatRule은 NONE/MONTHLY를 포함한 legacy 호환 필드입니다."
    )
    ResponseEntity<AdminQuestResponse.Definitions> definitions();

    @Operation(summary = "Quest Definition 생성/보장", description = "Blueprint code 기준으로 Quest Definition을 생성(또는 존재 보장)합니다.")
    ResponseEntity<AdminQuestResponse.Definition> ensure(
            @Valid @RequestBody AdminQuestRequest.Ensure request
    );

    @Operation(summary = "Quest Definition 단건 조회", description = "questCode로 Quest Definition을 조회합니다.")
    ResponseEntity<AdminQuestResponse.Definition> definition(String questCode);

    @Operation(
            summary = "Quest Definition 수정",
            description = "타겟/버전/Profile 참조/semantic category/progress source/repeat policy/선택적 Role 맥락을 부분 수정합니다. null은 no-change이며 repeatRule은 legacy 호환 필드입니다."
    )
    ResponseEntity<AdminQuestResponse.Definition> update(
            String questCode,
            @Valid @RequestBody AdminQuestRequest.Update request
    );

    @Operation(summary = "Quest Acceptance 목록(quest 기준)", description = "특정 questCode에 대한 Acceptance 목록을 조회합니다. (status 필터 가능)")
    ResponseEntity<AdminQuestResponse.Acceptances> acceptances(String questCode, String status);

    @Operation(summary = "Quest Acceptance 단건 조회", description = "acceptanceId로 Acceptance 상세를 조회합니다.")
    ResponseEntity<AdminQuestResponse.Acceptance> acceptance(Long acceptanceId);

    @Operation(summary = "Acceptance 진행도 조정", description = "운영/CS 목적으로 Acceptance 진행도를 non-negative delta로 조정합니다.")
    ResponseEntity<ApiResponse<AdminQuestResponse.Acceptance>> adjustProgress(
            @PathVariable @Positive Long acceptanceId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @Valid @RequestBody AdminQuestRequest.AdjustProgress request
    );

    @Operation(summary = "Acceptance 상태 변경", description = "허용된 상태 전이만 수행합니다. DONE 입력은 COMPLETED로 해석합니다.")
    ResponseEntity<ApiResponse<AdminQuestResponse.Acceptance>> changeStatus(
            @PathVariable @Positive Long acceptanceId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @Valid @RequestBody AdminQuestRequest.ChangeStatus request
    );
}
