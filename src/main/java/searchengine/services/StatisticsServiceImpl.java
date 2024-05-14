package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class StatisticsServiceImpl implements StatisticsService {
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;

    @Autowired
    public StatisticsServiceImpl(PageRepository pageRepository, SiteRepository siteRepository,
                                 LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public StatisticsResponse getStatistics() throws SQLException {
        List<searchengine.model.Site> sitesList = siteRepository.findAll();
        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesList.size());
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site site : sitesList) {
            if (site.getStatus().toString().equals("INDEXING")) {
                total.setIndexing(true);
            } else {
                total.setIndexing(false);
            }
            List<Page> pageList = pageRepository.findBySite_id(site.getId());
            int pages = pageList.size();
            List<Lemma> lemmaList = lemmaRepository.findBySite_id(site.getId());
            int lemmas = lemmaList.size();

            LocalDateTime localDateTime = site.getStatus_time();
            ZonedDateTime zdt = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
            long date = zdt.toInstant().toEpochMilli();
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(fillDetailedStatisticsItem(site.getUrl(), site.getName(), site.getStatus().toString(),
                    date, site.getLast_error(), pages, lemmas));
        }
        return fillStatisticsResponse(total, detailed);
    }

    private DetailedStatisticsItem fillDetailedStatisticsItem(String url, String name, String status,
                                                              long statusTime, String error, int pages, int lemmas) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setUrl(url);
        item.setName(name);
        item.setStatus(status);
        item.setStatusTime(statusTime);
        item.setError(error);
        item.setPages(pages);
        item.setLemmas(lemmas);
        return item;
    }

    private StatisticsResponse fillStatisticsResponse(TotalStatistics total,
                                                      List<DetailedStatisticsItem> detailed) {
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
