package online.lifeasgame.social.application.query;

import online.lifeasgame.social.domain.*;

import java.util.List;

public interface GuildQueryRepository {
    List<Guild> search(String keyword, GuildVisibility visibility, int page, int size);

    long countSearch(String keyword, GuildVisibility visibility);

    List<Guild> recent(int limit);
}
