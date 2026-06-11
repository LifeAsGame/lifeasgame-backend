package online.lifeasgame.character.application.view;

import online.lifeasgame.character.domain.CertificationCategory;

import java.time.Instant;
import java.time.LocalDate;

public interface PlayerCertificationView {

    Long getCertificationId();

    String getName();

    String getIssuer();

    CertificationCategory getCategory();

    LocalDate getAcquiredDate();

    LocalDate getExpiresDate();

    Instant getGrantedAt();
}
