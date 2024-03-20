package searchengine.config;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;
import searchengine.dto.IndexingPage.IndexingPageResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.services.LemmaFinder;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Beans StatisticsService
 */
@Configuration
public class Config {
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
    /**
     * Beans SearchServices
     */
    @Bean
    @Scope
    public AtomicBoolean indexingIsGone() {
        return new AtomicBoolean();
    }
}
