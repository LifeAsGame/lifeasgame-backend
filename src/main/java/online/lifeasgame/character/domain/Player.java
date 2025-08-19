package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.entity.AbstractTime;

@Entity
@Table(name = "player",
        indexes = @Index(name = "player_idx_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false, unique=true)
    private Long userId;                 // user ↔ player 1:1 유지(UNIQUE)

    @Column(length=40, nullable=false)
    private String name;

    @Column(length=10)
    private String gender;

    @Column(length=30)
    private String job;                  // n잡은 JSON로 확장 가능

    @Column(name="guild_id")
    private Long guildId;                // social 경계: ID 참조

    @Column(nullable=false) private int level = 1;
    @Column(nullable=false) private long exp = 0;
    @Column(nullable=false) private int hp = 100;
    @Column(nullable=false) private int mp = 50;

    // 기본/확장 스탯
    @Column(name="str_stat", nullable=false) private int strStat = 1;
    @Column(name="agi_stat", nullable=false) private int agiStat = 1;
    @Column(name="dex_stat", nullable=false) private int dexStat = 1;
    @Column(name="int_stat", nullable=false) private int intStat = 1;
    @Column(name="vit_stat", nullable=false) private int vitStat = 1;
    @Column(name="luc_stat", nullable=false) private int lucStat = 1;

    @Column(name="extra_stats", columnDefinition="json")
    private String extraStatsJson;       // 부가 스탯(사교력/노력 등)

    @Column(name="status_effects", columnDefinition="json")
    private String statusEffectsJson;    // 중독/혼란 등

    @Column(name="title_id")
    private Long titleId;                // 대표 칭호 ID

    public Player(Long id, Long userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
    }

    public static Player create(Long userId, String name){
        return new Player(null, userId, name);
    }
}
