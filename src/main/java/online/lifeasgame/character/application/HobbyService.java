package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.HobbyResult;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HobbyService {

    private final HobbyReader hobbyReader;

    public List<HobbyResult.HobbyInfo> getHobbies(List<String> categories) {
        List<Hobby> hobbies = hobbyReader.getHobbies(HobbyCategory.parse(categories));
        return HobbyResult.HobbyInfo.fromList(hobbies);
    }
}
