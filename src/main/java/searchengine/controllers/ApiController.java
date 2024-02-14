package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;


import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private SearchServices searchServices;
    private boolean indexingIsGone = false;
    private final SitesList sites;

    public ApiController(StatisticsService statisticsService, PageIndexingService pageIndexingService, SearchServices searchServices,  SitesList sites) {
        this.statisticsService = statisticsService;
        this.pageIndexingService = pageIndexingService;
        this.sites = sites;
        this.searchServices = searchServices;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() throws SQLException {
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
                    siteIndexingService.siteIndexing(siteRepository, pageRepository, newSite, lemmaRepository, indexRepository);
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

    @PostMapping("/indexPage")
    public HashMap<String, Object> indexPage(@RequestBody String url) throws IOException, InterruptedException {
        HashMap<String, Object> response = new HashMap<>();
        String regex = "url=";
        String regex1 = "%3A%2F%2F";
        String regex2 = "%2F";
        String url1 = url.replace(regex, "").replace(regex1, "://").replace(regex2, "/");

        URL url2 = new URL(url1);

        for (Site site : sites.getSites()) {
            List<Page> pages1 = pageRepository.findByPath(url2.getPath());

            Pattern pattern1 = Pattern.compile(url2.getHost());
            Matcher matcher1 = pattern1.matcher(site.getUrl());
            for (Page page : pages1) {
                if ((matcher1.find()) && (page.getPath().equals(url2.getPath()))
                        && (page.getSite().getUrl().contains(url2.getHost()))) {

                    List<Index> indexes = indexRepository.findByPage_id(page.getId());
                    for (Index index : indexes) {
                        Lemma lemmaNew = lemmaRepository.findById(index.getLemma().getId()).get();
                        lemmaNew.setFrequency(lemmaNew.getFrequency() - 1);
                        lemmaRepository.saveAndFlush(lemmaNew);
                    }
                    pageRepository.delete(page);

                   pageIndexingService.pageIndexing(url2.toString(), site);
                   response.put("result", Boolean.valueOf(true));
                   return response;
                }
            }
            List<Page> pages = pageRepository.findByPath(url2.getPath());

            Pattern pattern = Pattern.compile(url2.getHost());
            Matcher matcher = pattern.matcher(site.getUrl());

            if ((matcher.find()) && (pages.size() == 0)) {
                pageIndexingService.pageIndexing(url2.toString(), site);
                response.put("result", Boolean.valueOf(true));
                return response;
            }
            Pattern pattern2 = Pattern.compile(url2.getHost());
            Matcher matcher2 = pattern2.matcher(site.getUrl());

            for (Page page : pages){
                if ((matcher2.find()) && (page.getPath().equals(url2.getPath()))
                        && !(page.getSite().getUrl().contains(url2.getHost()))) {
                    pageIndexingService.pageIndexing(url2.toString(), site);
                    response.put("result", Boolean.valueOf(true));
                    return response;
                }
            }
        }


            response.put("result", false);
            response.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");

        return response;
    }


    @GetMapping("/search")
    public SearchResponse search(String query, String offset, String limit, String site) throws SQLException, IOException, InterruptedException {
        return searchServices.getSearch(query, offset, limit, site);
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





