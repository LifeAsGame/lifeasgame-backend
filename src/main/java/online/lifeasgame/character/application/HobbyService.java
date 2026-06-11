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
        List<Hobby> hobbies = hobbyReader.getByCategories(HobbyCategory.parse(categories));
        return HobbyResult.Info.fromList(hobbies);
    }

    @Transactional
    public HobbyResult.Info create(HobbyCommand.Create command) {
        Hobby hobby = hobbyWriter.create(
                Hobby.create(
                        command.name(),
                        HobbyCategory.parse(command.category())
                )
        );

        return HobbyResult.Info.from(hobby);
    }

    public HobbyResult.Info getHobby(Long hobbyId) {
        Hobby hobby = hobbyReader.getByIdOrThrow(hobbyId);
        return HobbyResult.Info.from(hobby);
    }

    @Transactional
    public HobbyResult.Info update(Long hobbyId, HobbyCommand.Update command) {
        HobbyCategory category = HobbyCategory.parse(command.category());
        Hobby hobby = hobbyReader.getByIdOrThrow(hobbyId);
        hobby.update(
                command.name(),
                category
        );

        return HobbyResult.Info.from(hobby);
    }

    @Transactional
    public HobbyResult.Deleted delete(Long hobbyId) {
        hobbyWriter.delete(hobbyId);
        return new HobbyResult.Deleted(hobbyId);
    }
}
