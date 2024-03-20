package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;
import searchengine.dto.IndexingPage.IndexingPageResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;
import java.io.IOException;
import java.sql.SQLException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private StatisticsService statisticsService;
    private PageIndexingService pageIndexingService;
    private SiteIndexingService siteIndexingService;
    private SearchServices searchServices;

    @Autowired
    public ApiController(StatisticsService statisticsService, PageIndexingService pageIndexingService,
                         SiteIndexingService siteIndexingService, SearchServices searchServices) {
        this.statisticsService = statisticsService;
        this.pageIndexingService = pageIndexingService;
        this.siteIndexingService = siteIndexingService;
        this.searchServices = searchServices;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() throws SQLException {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public IndexingStartResponse startIndexing() throws IOException, InterruptedException {
        return siteIndexingService.indexingStartResponse();
    }

    @GetMapping("/stopIndexing")
    public IndexingStopResponse stopIndexing() {
        return siteIndexingService.indexingStopResponse();
    }

    @PostMapping("/indexPage")
    public IndexingPageResponse indexPage(@RequestBody String url) throws IOException, InterruptedException {
        return pageIndexingService.pageIndexing(url);
    }


    @GetMapping("/search")
    public SearchResponse search(String query, String offset, String limit, String site) throws SQLException, IOException, InterruptedException {
        return searchServices.getSearch(query, offset, limit, site);
    }
}

