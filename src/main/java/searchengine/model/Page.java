package searchengine.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import javax.persistence.Index;
import java.util.List;

@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path"))
@NoArgsConstructor
@Getter
@Setter
public class Page {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;

    @Column(nullable = false, columnDefinition = "INT")
    private Integer code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<searchengine.model.Index> indices;
}
