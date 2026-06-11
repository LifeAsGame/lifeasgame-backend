package online.lifeasgame.character.application.view;

import online.lifeasgame.character.domain.HobbyCategory;
import online.lifeasgame.character.domain.PlayerHobbyStatus;

import java.time.LocalDate;

public interface PlayerHobbyView {

    Long getHobbyId();

    String getName();

    HobbyCategory getCategory();

    String getCustomName();

    String getDetail();

    int getProficiency();

    PlayerHobbyStatus getStatus();

    LocalDate getStartedOn();

    long getXp();
}
