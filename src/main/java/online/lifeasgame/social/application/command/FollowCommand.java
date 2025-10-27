package online.lifeasgame.social.application.command;

public final class FollowCommand {
    public record Create(Long targetPlayerId) {
        public static Create of(Long targetPlayerId){ return new Create(targetPlayerId); }
    }
    public record Unfollow() {
        public static Unfollow of(){ return new Unfollow(); }
    }
    public record Mute() {
        public static Mute of(){ return new Mute(); }
    }
    public record Unmute() {
        public static Unmute of(){ return new Unmute(); }
    }
    public record Block() {
        public static Block of(){ return new Block(); }
    }
    public record Unblock() {
        public static Unblock of(){ return new Unblock(); }
    }
}
