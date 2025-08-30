package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@Table(name="titles")
public class Title extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, unique = true)
    private String code;

    @Column(length = 60, nullable = false)
    private String name;

    @Lob
    @Column(name = "desc_md")
    private String descMd;
}
