package online.lifeasgame.user.application.model;

import online.lifeasgame.core.guard.Guard;

public record RawPassword(String value) {
    public RawPassword {
        Guard.notNull(value, "password");
        var v = value.strip();
        Guard.minLength(v, 8, "password");
        Guard.maxLength(v, 72, "password");
        Guard.check(v.matches("^[a-zA-Z0-9]*$"), "password contains invalid characters");
    }

    public static RawPassword of(String value) {
        return new RawPassword(value);
    }
}
