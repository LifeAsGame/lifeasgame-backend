package online.lifeasgame.social.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.time.CalendarDateRange;
import online.lifeasgame.social.domain.error.SocialError;

import java.time.Instant;

public record GroupActivityDetails(String title, String sharedDescription, String location,
                                   Instant startsAt, Instant endsAt) {
    public GroupActivityDetails {
        if (title == null || title.isBlank() || title.strip().length() > 120
                || sharedDescription != null && sharedDescription.length() > 2000
                || location != null && location.length() > 200
                || startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)
                || !CalendarDateRange.inDateTimeColumn(startsAt)
                || !CalendarDateRange.inDateTimeColumn(endsAt))
            throw new DomainException(SocialError.GROUP_ACTIVITY_INVALID_INPUT);
        title = title.strip();
        location = location == null || location.isBlank() ? null : location.strip();
    }
}
