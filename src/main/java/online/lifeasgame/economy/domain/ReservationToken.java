package online.lifeasgame.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationToken {

    @Column(name = "reservation_token", length = 36)
    private String value;

    private ReservationToken(String v){
        this.value = Guard.maxLength(Guard.notBlank(v, "reservationToken"), 36, "reservationToken");
    }

    public static ReservationToken newToken() {
        return new ReservationToken(UUID.randomUUID().toString());
    }

    public static ReservationToken of(String v) {
        return new ReservationToken(v);
    }

    public String value() {
        return value;
    }
}
