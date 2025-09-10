package online.lifeasgame.character.presentation.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AdminPlayerRequest {

    private AdminPlayerRequest() {
    }

    public record GrantExp(
            @NotNull @Positive Long expDelta
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
        @AssertTrue(message = "at least one positive delta required")
        public boolean isAnyPositive() {
            return (strDelta != null && strDelta > 0)
                    || (agiDelta != null && agiDelta > 0)
                    || (dexDelta != null && dexDelta > 0)
                    || (intelDelta != null && intelDelta > 0)
                    || (vitDelta != null && vitDelta > 0)
                    || (lucDelta != null && lucDelta > 0);
        }

        public int nStr() { return strDelta == null ? 0 : strDelta; }
        public int nAgi() { return agiDelta == null ? 0 : agiDelta; }
        public int nDex() { return dexDelta == null ? 0 : dexDelta; }
        public int nInt() { return intelDelta == null ? 0 : intelDelta; }
        public int nVit() { return vitDelta == null ? 0 : vitDelta; }
        public int nLuc() { return lucDelta == null ? 0 : lucDelta; }
    }
}
