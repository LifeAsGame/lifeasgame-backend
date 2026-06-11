package online.lifeasgame.character.domain;

public record CoreStatDelta(int str, int agi, int dex, int intel, int vit, int luc) {

    public boolean isZero() {
        return str == 0 && agi == 0 && dex == 0 && intel == 0 && vit == 0 && luc == 0;
    }

    public static CoreStatDelta of(int str, int agi, int dex, int intel, int vit, int luc) {
        return new CoreStatDelta(str, agi, dex, intel, vit, luc);
    }
}
