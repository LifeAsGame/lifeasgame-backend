package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.HobbyCommand;
import online.lifeasgame.character.application.result.HobbyResult;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HobbyService {

    private final HobbyReader hobbyReader;
    private final HobbyWriter hobbyWriter;

    public List<HobbyResult.Info> getHobbies(List<String> categories) {
        List<Hobby> hobbies = hobbyReader.getHobbies(HobbyCategory.parse(categories));
        return HobbyResult.Info.fromList(hobbies);
    }

    @Transactional
    public HobbyResult.Info create(HobbyCommand.Create command) {
        Hobby hobby = hobbyWriter.create(
                Hobby.of(
                        command.name(),
                        HobbyCategory.parse(command.category())
                )
        );

        return HobbyResult.Info.of(
                hobby.getId(),
                hobby.getName(),
                hobby.getCategory().name()
        );
    }
}
