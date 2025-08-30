package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@AggregateRoot
@Table(name = "user_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSetting extends AbstractTime {

    @Id
    @Column(name="user_id")
    private Long userId;

    @Embedded
    private Volume volume;

    @Column(name="ui_layout", columnDefinition="json")
    private String uiLayoutJson;

    @Column(name="flags", columnDefinition="json")
    private String flagsJson;

    private UserSetting(Long userId, Volume volume) {
        this.userId = userId;
        this.volume = volume;
    }

    public static UserSetting of(Long userId){
        return new UserSetting(userId, Volume.of(50));
    }
}
