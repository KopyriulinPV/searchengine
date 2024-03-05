package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Setter
@Getter
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;
}
