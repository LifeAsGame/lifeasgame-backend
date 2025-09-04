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

    @Column(name = "str_stat", nullable = false)
    private int str = 1;

    @Column(name = "agi_stat", nullable = false)
    private int agi = 1;

    @Column(name = "dex_stat", nullable = false)
    private int dex = 1;

    @Column(name = "int_stat", nullable = false)
    private int intel = 1;

    @Column(name = "vit_stat", nullable = false)
    private int vit = 1;

    @Column(name = "luc_stat", nullable = false)
    private int luc = 1;

    private CoreStats(int str, int agi, int dex, int intel, int vit, int luc) {
        Guard.minValue(str, 1, "str");
        Guard.minValue(agi, 1, "agi");
        Guard.minValue(dex, 1, "dex");
        Guard.minValue(intel, 1, "intel");
        Guard.minValue(vit, 1, "vit");
        Guard.minValue(luc, 1, "luc");
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
}
