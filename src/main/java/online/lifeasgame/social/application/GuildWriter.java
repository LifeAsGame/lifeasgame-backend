package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.repository.GuildRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class GuildWriter {

    private final GuildRepository repository;

    public Guild create(Guild guild) {
        return repository.save(guild);
    }
}
