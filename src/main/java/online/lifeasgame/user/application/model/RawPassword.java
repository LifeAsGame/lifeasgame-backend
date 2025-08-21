package online.lifeasgame.user.application.model;

import online.lifeasgame.shared.guard.Guard;

public record RawPassword(String value) {
    public RawPassword {
        Guard.notNull(value, "password");
        var v = value.trim();
        if (!v.matches("^[a-zA-Z0-9]*$")) {
            throw new IllegalArgumentException("password contains invalid characters");
        }
        if (v.length() < 8) {
            throw new IllegalArgumentException("password length >= 8");
        }
    }
}
