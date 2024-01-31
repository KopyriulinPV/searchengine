package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;


import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteIndexingService;
import searchengine.services.SiteIndexingServiceImpl;
import searchengine.services.StatisticsService;
import searchengine.services.TravelingTheWeb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;

    private StatisticsService statisticsService;
    private boolean indexingIsGone = false;
    private final SitesList sites;


    public ApiController(StatisticsService statisticsService, SitesList sites) {
        this.statisticsService = statisticsService;
        this.sites = sites;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());

    }

    @GetMapping("/startIndexing")
    public HashMap<String, Object> startIndexing()
    {
        TravelingTheWeb.indexingStop = false;
        HashMap<String, Object> response = new HashMap<>();
        if (indexingIsGone) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return response;
        }
        indexingIsGone = true;

        for (Site site : sites.getSites()) {

            Iterable<searchengine.model.Site> iterable = siteRepository.findAll();
            Iterator<searchengine.model.Site> siteIterator = iterable.iterator();
            while (siteIterator.hasNext()) {
                searchengine.model.Site iteratorNext = siteIterator.next();
                if (iteratorNext.getUrl().equals(site.getUrl())) {
                    siteRepository.delete(iteratorNext);
                }
            }

            new Thread(()-> {
            searchengine.model.Site newSite = new searchengine.model.Site();
            newSite.setName(site.getName());
            newSite.setUrl(site.getUrl());
            SiteIndexingService siteIndexingService = new SiteIndexingServiceImpl();
                try {
                    siteIndexingService.siteIndexing(siteRepository, pageRepository, newSite);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        }

        response.put("result", Boolean.valueOf(true));
        return response;
    }

    @GetMapping("/stopIndexing")
    public HashMap<String, Object> stopIndexing()
    {
        indexingIsGone = false;
        HashMap<String, Object> response1 = new HashMap<>();
        if (!indexingIsGone) {
            TravelingTheWeb.indexingStop = true;

            response1.put("result", Boolean.valueOf(true));
        } else {
            response1.put("result", false);
            response1.put("error", "Индексация не запущена");

        }
        return response1;
    }
 }

/*indexing-settings:
        sites:
        - url: https://www.lenta.ru
        name: Лента.ру
        - url: https://volochek.life/
        name: Volochek
        - url: https://www.playback.ru
        name: PlayBack.Ru*/

    /*show-sql: true*/





