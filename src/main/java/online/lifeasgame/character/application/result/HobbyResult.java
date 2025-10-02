package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.Hobby;

import java.util.List;

public final class HobbyResult {

    private HobbyResult() {
    }

    public record Info(
            Long hobbyId,
            String name,
            String category
    ) {
        public static Info from(Hobby hobby) {
            return new Info(
                    hobby.getId(),
                    hobby.getName(),
                    hobby.getCategory().name()
            );
        }

        public static List<Info> fromList(List<Hobby> hobbies) {
            return hobbies.stream().map(Info::from).toList();
        }

        public static Info of(Long hobbyId, String name, String category) {
            return new Info(hobbyId, name, category);
        }
    }
}
