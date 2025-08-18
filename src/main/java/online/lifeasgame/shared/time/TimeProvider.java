package online.lifeasgame.shared.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public interface TimeProvider {
    Instant now();
    default ZoneId zone() { return ZoneOffset.UTC; }
}
