package online.lifeasgame.lifelog.application.command;

import jakarta.validation.constraints.Min;

import java.util.Set;

public final class MediaLogCommand {

    private MediaLogCommand() {
    }

    public record Create(
            String category,
            String title,
            String originalTitle,
            Integer currentEpisode,
            Integer totalEpisode,
            String status,
            Set<String> tags
    ) {
    }

    public record Rate(Double score) {
    }

    public record Advance(Integer step) {
    }

    public record MarkStatus(String status) {
    }

    public record Search(
            String category,
            String status,
            String titleLike,
            int page,
            int size
    ) {
    }

    public record Update(
            String category,
            String title,
            String originalTitle,
            @Min(0) Integer currentEpisode,
            @Min(1) Integer totalEpisode,
            String status,
            Set<String> tags
    ) {
    }
}
