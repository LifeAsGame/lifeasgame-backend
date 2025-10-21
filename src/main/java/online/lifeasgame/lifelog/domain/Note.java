package online.lifeasgame.lifelog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Note {
    @Column(name = "note", length = 1000)
    private String value;

    protected Note() {
    }

    private Note(String v) {
        this.value = v;
    }

    public static Note of(String v) {
        if (v == null) return null;
        if (v.length() > 1000) throw new IllegalArgumentException("note too long");
        return new Note(v);
    }

    public String value() {
        return value;
    }
}
