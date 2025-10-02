package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.Hobby;

import java.util.List;

public class HobbyResult {

    private HobbyResult() {
    }

    public record HobbyInfo(
            Long hobbyId,
            String name,
            String category
    ) {
        public static HobbyInfo from(Hobby hobby) {
            return new HobbyInfo(
                    hobby.getId(),
                    hobby.getName(),
                    hobby.getCategory().name()
            );
        }

        public static List<HobbyInfo> fromList(List<Hobby> hobbies) {
            return hobbies.stream().map(HobbyResult.HobbyInfo::from).toList();
        }

        public static HobbyInfo of(Long hobbyId, String name, String category) {
            return new HobbyInfo(hobbyId, name, category);
        }
    }
}
