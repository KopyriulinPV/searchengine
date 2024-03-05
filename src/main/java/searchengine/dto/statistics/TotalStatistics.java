package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;
}
