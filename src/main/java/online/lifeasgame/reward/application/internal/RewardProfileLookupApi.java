package online.lifeasgame.reward.application.internal;

public interface RewardProfileLookupApi {

    RewardProfileReference getActiveByCode(String code);

    record RewardProfileReference(String code) {
    }
}
