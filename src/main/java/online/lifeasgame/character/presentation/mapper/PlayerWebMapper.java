package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.presentation.request.PlayerRequest;
import online.lifeasgame.character.presentation.response.PlayerResponse;

public class PlayerWebMapper {

    private PlayerWebMapper() {
    }

    public static PlayerCommand.Register toCommand(PlayerRequest.Register request) {
        return PlayerCommand.Register.of(request.name(), request.gender());
    }

    public static PlayerCommand.ChangeHp toCommand(Long playerId, PlayerRequest.ChangeHp request) {
        return PlayerCommand.ChangeHp.of(playerId, request.hpDelta());
    }

    public static PlayerCommand.ChangeHpCapacity toCommand(Long playerId, PlayerRequest.ChangeHpCapacity request) {
        return PlayerCommand.ChangeHpCapacity.of(playerId, request.hpCapacityDelta());
    }

    public static PlayerCommand.ChangeMp toCommand(Long playerId, PlayerRequest.ChangeMp request) {
        return PlayerCommand.ChangeMp.of(playerId, request.mpDelta());
    }

    public static PlayerCommand.ChangeMpCapacity toCommand(Long playerId, PlayerRequest.ChangeMpCapacity request) {
        return PlayerCommand.ChangeMpCapacity.of(playerId, request.mpCapacityDelta());
    }


    public static PlayerResponse.Created toCreated(PlayerResult.Created result) {
        return new PlayerResponse.Created(result.id());
    }

    public static PlayerResponse.PlayerInfo toPlayerInfo(PlayerResult.PlayerInfo result) {
        return PlayerResponse.PlayerInfo.of(
                result.name(),
                result.gender(),
                result.job(),
                result.level(),
                result.exp(),
                result.currentHealth(),
                result.healthCapacity(),
                result.currentMana(),
                result.manaCapacity(),
                result.str(),
                result.agi(),
                result.dex(),
                result.intel(),
                result.vit(),
                result.luc(),
                result.extraStats(),
                result.effects().stream()
                        .map((effect) -> PlayerResponse.PlayerInfo.StatusEffects.of(effect.name(), effect.category().name()))
                        .toList()
        );
    }

    public static PlayerResponse.CurrentHp toCurrentHp(PlayerResult.CurrentHp result) {
        return PlayerResponse.CurrentHp.of(
                result.value()
        );
    }

    public static PlayerResponse.HpCapacity toHpCapacity(PlayerResult.HpCapacity result) {
        return PlayerResponse.HpCapacity.of(
                result.cap()
        );
    }

    public static PlayerResponse.CurrentMp toCurrentMp(PlayerResult.CurrentMp result) {
        return PlayerResponse.CurrentMp.of(
                result.value()
        );
    }

    public static PlayerResponse.MpCapacity toMpCapacity(PlayerResult.MpCapacity result) {
        return PlayerResponse.MpCapacity.of(
                result.cap()
        );
    }
}
