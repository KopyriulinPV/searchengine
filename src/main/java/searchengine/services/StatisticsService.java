package searchengine.services;

import searchengine.dto.statistics.StatisticsResponse;
import java.sql.SQLException;

public interface StatisticsService {
    StatisticsResponse getStatistics() throws SQLException;
}
