package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
