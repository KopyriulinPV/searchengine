package searchengine.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "lemmas")
@NoArgsConstructor
@Getter
@Setter
public class Lemma {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "lemma", nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(nullable = false, columnDefinition = "INT")
    private Integer frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<Index> indexes;
}
