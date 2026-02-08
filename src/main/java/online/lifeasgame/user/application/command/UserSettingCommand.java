package online.lifeasgame.user.application.command;

public final class UserSettingCommand {

    private UserSettingCommand() {}

    public record UpdateSettings(
            Integer volume,
            String uiLayoutJson,
            String flagsJson
    ) {
    }
}
