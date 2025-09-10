package online.lifeasgame.character.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PlayerRequest {

    private PlayerRequest() {
    }

    public record Register(
            @NotBlank @Size(min = 1, max = 20) String name,
            @NotBlank String gender
    ) {
    }

    public record ChangeHp (
            @NotNull Integer hpDelta
    ){
    }

    public record ChangeHpCapacity(
            @NotNull Integer hpCapacityDelta
    ) {
    }

    public record ChangeMp (
            @NotNull Integer mpDelta
    ){
    }

    public record ChangeMpCapacity(
            @NotNull Integer mpCapacityDelta
    ) {
    }
}
