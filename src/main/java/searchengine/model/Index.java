package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;


@Entity
@Table(name = "indexes")
@NoArgsConstructor
@Getter
@Setter
public class Index {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(nullable = true, columnDefinition = "FLOAT")
    private Integer lemma_rank;
}



