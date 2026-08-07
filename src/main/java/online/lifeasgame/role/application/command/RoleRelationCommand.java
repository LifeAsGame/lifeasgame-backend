package online.lifeasgame.role.application.command;

public final class RoleRelationCommand {

    private RoleRelationCommand() {
    }

    public record Create(
            Long personId,
            String relationType,
            String roleNotes
    ) {
    }

    public record Update(String relationType, String roleNotes) {
    }
}
