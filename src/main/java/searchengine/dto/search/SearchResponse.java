package searchengine.dto.search;

import lombok.Data;
import searchengine.dto.statistics.DetailedStatisticsItem;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private Integer count;
    private List<searchengine.dto.search.Data> data;
}
