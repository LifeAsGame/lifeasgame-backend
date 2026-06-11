package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoreStats {

    public static final int MIN = 1;
    public static final int MAX = 9999;

    @Column(name = "str_stat", nullable = false)
    private int str;

    @Column(name = "agi_stat", nullable = false)
    private int agi;

    @Column(name = "dex_stat", nullable = false)
    private int dex;

    @Column(name = "int_stat", nullable = false)
    private int intel;

    @Column(name = "vit_stat", nullable = false)
    private int vit;

    @Column(name = "luc_stat", nullable = false)
    private int luc;

    private CoreStats(int str, int agi, int dex, int intel, int vit, int luc) {
        Guard.minValue(str, 1, "str");
        Guard.minValue(agi, 1, "agiDelta");
        Guard.minValue(dex, 1, "dexDelta");
        Guard.minValue(intel, 1, "intelDelta");
        Guard.minValue(vit, 1, "vitDelta");
        Guard.minValue(luc, 1, "lucDelta");
        this.str = str;
        this.agi = agi;
        this.dex = dex;
        this.intel = intel;
        this.vit = vit;
        this.luc = luc;
    }

    public static CoreStats of(int str, int agi, int dex, int intel, int vit, int luc) {
        return new CoreStats(str, agi, dex, intel, vit, luc);
    }

    public static CoreStats defaults() {
        return new CoreStats(1, 1, 1, 1, 1, 1);
    }

    public int str() { return str; }

    public int agi(){ return agi; }

    public int dex(){ return dex; }

    public int intel(){ return intel; }

    public int vit(){ return vit; }

    public int luc(){ return luc; }

    public CoreStats grant(CoreStatDelta d) {
        if (d == null || d.isZero()) {
            return this;
        }

        return new CoreStats(
                addClamp(this.str, d.str()),
                addClamp(this.agi, d.agi()),
                addClamp(this.dex, d.dex()),
                addClamp(this.intel, d.intel()),
                addClamp(this.vit, d.vit()),
                addClamp(this.luc, d.luc())
        );
    }

    private static int addClamp(int base, int delta) {
        long sum = (long) base + (long) delta;
        if (sum >= MAX) {
            return MAX;
        } else if (sum <= MIN) {
            return MIN;
        }
        return (int) sum;
    }
}
