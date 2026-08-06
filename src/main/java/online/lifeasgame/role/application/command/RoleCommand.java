package online.lifeasgame.role.application.command;

public final class RoleCommand {

    private RoleCommand() {
    }

    public record Create(String roleType, String name, String description) {
    }

    public record Update(String roleType, String name, String description) {
    }
}
