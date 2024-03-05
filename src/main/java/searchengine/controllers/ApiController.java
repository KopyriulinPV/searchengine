package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import searchengine.config.Config;
import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;
import searchengine.dto.IndexingPage.IndexingPageResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;

import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.*;

import java.io.IOException;

import java.sql.SQLException;


@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

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
        IndexingStartResponse indexingStartResponse = siteIndexingService.indexingStartResponse();
        siteIndexingService.siteIndexing(siteRepository, pageRepository, lemmaRepository, indexRepository);
        return indexingStartResponse;
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

