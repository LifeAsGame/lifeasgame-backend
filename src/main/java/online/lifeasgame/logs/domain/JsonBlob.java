package online.lifeasgame.logs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class JsonBlob {

    @Column(name = "json", columnDefinition = "json", nullable = false)
    private String value;

    private JsonBlob(String v) {
        String s = Guard.notBlank(v, "json").trim();
        this.value = s;
    }

    public static JsonBlob of(String v) {
        return new JsonBlob(v);
    }

    public String value() {
        return value;
    }
}
