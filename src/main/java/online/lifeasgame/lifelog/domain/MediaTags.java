package online.lifeasgame.lifelog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaTags {

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "media_log_tags", joinColumns = @JoinColumn(name = "media_log_id"))
    @Column(name = "tag", length = 50)
    private Set<String> values = new LinkedHashSet<>();

    private MediaTags(Set<String> values) {
        if (values != null) {
            this.values = values.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toLowerCase())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public static MediaTags of(Set<String> values) {
        return new MediaTags(values);
    }

    public Set<String> values() {
        return Set.copyOf(values);
    }
}
