package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@AggregateRoot
@Table(name = "hobbies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hobby extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    HobbyCategory category;

    @Column(name = "source", nullable = false, length = 32)
    String source = "LEGACY";

    @Column(name = "active", nullable = false)
    boolean active = true;

    public Hobby(String name, HobbyCategory category) {
        this.name = name;
        this.category = category;
    }

    public static Hobby create(String name, HobbyCategory category) {
        return new Hobby(name, category);
    }

    public void update(String name, HobbyCategory category) {
        this.name = name;
        this.category = category;
    }

    public void deactivate() {
        this.active = false;
    }
}
