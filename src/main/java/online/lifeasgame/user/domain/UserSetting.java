package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.entity.AbstractTime;

@Entity
@Table(name = "user_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSetting extends AbstractTime {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(length=10)
    private String language = "ko";

    private Integer volume = 50;

    @Column(name="ui_layout", columnDefinition="json")
    private String uiLayoutJson;

    @Column(name="flags", columnDefinition="json")
    private String flagsJson;

    public UserSetting(Long userId) {
        this.userId = userId;
    }

    public static UserSetting of(Long userId){
        return new UserSetting(userId);
    }
}
