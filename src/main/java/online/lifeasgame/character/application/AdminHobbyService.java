package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AdminHobbyCommand;
import online.lifeasgame.character.application.result.AdminHobbyResult;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class AdminHobbyService {

    private final HobbyWriter hobbyWriter;

    @Transactional
    public AdminHobbyResult.HobbyInfo create(AdminHobbyCommand.CreateHobby command) {
        Hobby hobby = hobbyWriter.create(
                Hobby.of(
                        command.name(),
                        HobbyCategory.parse(command.category())
                )
        );

        return AdminHobbyResult.HobbyInfo.of(
                hobby.getId(),
                hobby.getName(),
                hobby.getCategory().name()
        );
    }
}
