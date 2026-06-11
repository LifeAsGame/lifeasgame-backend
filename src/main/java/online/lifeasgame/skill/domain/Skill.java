package online.lifeasgame.skill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@AggregateRoot
@Table(name = "skills")
public class Skill extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private SkillCode code;

    @Embedded
    private SkillName name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SkillKind kind;

    @Column(name = "max_level", nullable = false)
    private int maxLevel = 5;

    @Convert(converter = BaseEffectConverter.class)
    @Column(name = "base_effect", columnDefinition = "json")
    private BaseEffect baseEffect;
}
