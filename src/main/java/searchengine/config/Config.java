package searchengine.config;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;
import searchengine.dto.IndexingPage.IndexingPageResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.services.LemmaFinder;
import searchengine.services.StatisticsServiceImpl;
import java.io.IOException;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
/**
 * Beans StatisticsService
 */
@Configuration
public class Config {
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = TARGET_CLASS)
    public StatisticsServiceImpl statisticsServiceImpll() {
        return new StatisticsServiceImpl(total(), statisticsResponse(), data());
    }

    @Bean
    @Scope(value = "prototype")
    public StatisticsResponse statisticsResponse() {
        return new StatisticsResponse();
    }

    @Bean
    @Scope(value = "prototype")
    public TotalStatistics total() {
        return new TotalStatistics();
    }

    @Bean
    @Scope(value = "prototype")
    public StatisticsData data() {
        return new StatisticsData();
    }
    /**
     * Beans SiteIndexingService
     */
    @Bean
    @Scope(value = "prototype")
    public IndexingStartResponse indexingStartResponse() {
        return new IndexingStartResponse();
    }

    @Bean
    @Scope(value = "prototype")
    public IndexingStopResponse indexingStopResponse() {
        return new IndexingStopResponse();
    }
    /**
     * Beans PageIndexingService
     */
    @Bean
    @Scope(value = "prototype")
    public IndexingPageResponse indexingPageResponse() {
        return new IndexingPageResponse();
    }
    /**
     * Beans SearchServices
     */
    @Bean
    @Scope(value = "prototype")
    public SearchResponse searchResponse() {
        return new SearchResponse();
    }
    /**
     * Beans для всех классов
     */
    @Bean
    @Scope(value = "prototype")
    public LemmaFinder lemmaFinder() throws IOException {
        return new LemmaFinder(new RussianLuceneMorphology());
    }
}
