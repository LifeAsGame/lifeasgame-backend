package online.lifeasgame.character.application.view;

import java.time.Instant;
import java.time.LocalDate;
import online.lifeasgame.character.domain.CertificationCategory;

public interface PlayerCertificationView {
    Long getCertificationId();
    String getName();
    String getIssuer();
    CertificationCategory getCategory();
    LocalDate getAcquiredDate();
    LocalDate getExpiresDate();
    Instant getGrantedAt();
}
