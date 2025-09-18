package online.lifeasgame.character.application.result;

import java.util.List;
import online.lifeasgame.character.domain.Hobby;

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
    }
}
