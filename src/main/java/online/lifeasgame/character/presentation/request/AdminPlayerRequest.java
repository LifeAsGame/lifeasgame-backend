package online.lifeasgame.character.presentation.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AdminPlayerRequest {

    private AdminPlayerRequest() {
    }

    public record GrantExp(
            @NotNull @Positive Long exp
    ) {
    }

    public record GrantCoreStats(
            Integer str,
            Integer agi,
            Integer dex,
            Integer intel,
            Integer vit,
            Integer luc
    ) {
        @AssertTrue(message = "at least one positive delta required")
        public boolean isAnyPositive() {
            return (str != null && str > 0)
                    || (agi != null && agi > 0)
                    || (dex != null && dex > 0)
                    || (intel != null && intel > 0)
                    || (vit != null && vit > 0)
                    || (luc != null && luc > 0);
        }

        public int nStr() { return str == null ? 0 : str; }
        public int nAgi() { return agi == null ? 0 : agi; }
        public int nDex() { return dex == null ? 0 : dex; }
        public int nInt() { return intel == null ? 0 : intel; }
        public int nVit() { return vit == null ? 0 : vit; }
        public int nLuc() { return luc == null ? 0 : luc; }
    }
}
