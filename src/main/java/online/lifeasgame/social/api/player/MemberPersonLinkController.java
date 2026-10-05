package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import online.lifeasgame.core.time.CalendarDateRange;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.domain.PersonProfile;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.platform.web.validation.DatabaseCalendarDate;
import online.lifeasgame.social.application.MemberPersonLinkService;
import online.lifeasgame.social.domain.PersonalGroupType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member-person-links")
public class MemberPersonLinkController {
    private final MemberPersonLinkService links;

    @GetMapping
    public ResponseEntity<ApiResponse<MemberPersonLinkService.Identity>> find(
            @RequestParam PersonalGroupType groupType, @RequestParam Long groupId,
            @RequestParam Long memberPlayerId) {
        return ApiResponses.ok(links.find(groupType, groupId, memberPlayerId));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<MemberPersonLinkService.Identity>> select(@Valid @RequestBody Select request) {
        return ApiResponses.ok(links.select(request.groupType(), request.groupId(),
                request.memberPlayerId(), request.personId()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberPersonLinkService.Identity>> create(@Valid @RequestBody Create request) {
        return ApiResponses.ok(links.create(request.groupType(), request.groupId(), request.memberPlayerId(),
                new PersonCommand.Create(request.displayName(), request.notes(), request.birthday(),
                        request.contact(), request.profile())));
    }

    public record Select(@NotNull PersonalGroupType groupType, @NotNull @Positive Long groupId,
                         @NotNull @Positive Long memberPlayerId, @NotNull @Positive Long personId) {}
    public record Create(@NotNull PersonalGroupType groupType, @NotNull @Positive Long groupId,
                         @NotNull @Positive Long memberPlayerId, String displayName, String notes,
                         @DatabaseCalendarDate LocalDate birthday, String contact, PersonProfile profile) {
        @JsonIgnore @AssertTrue(message = "Profile contains an invalid calendar date")
        public boolean isProfileDatesValid() {
            return profile == null || CalendarDateRange.inJson(profile.ageReferenceDate())
                    && CalendarDateRange.inJson(profile.firstMetOn())
                    && CalendarDateRange.inJson(profile.lastContactOn())
                    && (profile.importantDates() == null || profile.importantDates().stream()
                    .allMatch(date -> date == null || CalendarDateRange.inJson(date.date())));
        }
    }
}
