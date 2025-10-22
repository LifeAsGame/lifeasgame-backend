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
public class CollectionTags {

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "collection_log_tags", joinColumns = @JoinColumn(name = "collection_log_id"))
    @Column(name = "tag", length = 50)
    private Set<String> values = new LinkedHashSet<>();

    private CollectionTags(Set<String> values) {
        if (values != null) {
            this.values = values.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toLowerCase())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public static CollectionTags of(Set<String> values) {
        return new CollectionTags(values);
    }

    public Set<String> values() {
        return Set.copyOf(values);
    }
}
