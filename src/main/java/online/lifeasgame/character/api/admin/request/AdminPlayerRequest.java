package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class AdminPlayerRequest {

    private AdminPlayerRequest() {
    }

    public record GrantExp(
            @NotNull @Positive Long expDelta
    ) {
    }

    public record ChangeHp (
            @NotNull Integer hpDelta
    ){
    }

    public record ChangeHpCapacity(
            @NotNull Integer hpCapacityDelta
    ) {
    }

    public record ChangeMp (
            @NotNull Integer mpDelta
    ){
    }

    public record ChangeMpCapacity(
            @NotNull Integer mpCapacityDelta
    ) {
    }

    public record GrantCoreStats(
            Integer strDelta,
            Integer agiDelta,
            Integer dexDelta,
            Integer intelDelta,
            Integer vitDelta,
            Integer lucDelta
    ) {
        public int nStr() { return strDelta == null ? 0 : strDelta; }
        public int nAgi() { return agiDelta == null ? 0 : agiDelta; }
        public int nDex() { return dexDelta == null ? 0 : dexDelta; }
        public int nInt() { return intelDelta == null ? 0 : intelDelta; }
        public int nVit() { return vitDelta == null ? 0 : vitDelta; }
        public int nLuc() { return lucDelta == null ? 0 : lucDelta; }
    }

    public record GrantStatusEffects(
            @NotEmpty List<@NotBlank String> codes
    ) {
    }
}
