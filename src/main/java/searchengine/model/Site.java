
package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "site")
@NoArgsConstructor
@Getter
@Setter
public class Site {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private Status status;


    @Column(nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime status_time;

    @Column(columnDefinition = "TEXT")
    private String last_error;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<Page> pages;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<Lemma> lemmas;
}


