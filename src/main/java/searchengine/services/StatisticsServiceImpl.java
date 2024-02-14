package searchengine.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
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
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Data
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    @Override
    public StatisticsResponse getStatistics() throws SQLException {

        TotalStatistics total = new TotalStatistics();
        List<searchengine.model.Site> sitesList = siteRepository.findAll();

        total.setSites(sitesList.size());

        boolean indexing = false;
        for (Site site : sitesList) {
            if (site.getStatus().equals("INDEXING")) {
                indexing = true;
            }
        }
        total.setIndexing(indexing);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            List<Page> pageList = pageRepository.findBySite_id(site.getId());
            int pages = pageList.size();

            List<Lemma> lemmaList = lemmaRepository.findBySite_id(site.getId());
            int lemmas = lemmaList.size();
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus().toString());
            item.setError(site.getLast_error());

            LocalDateTime localDateTime = site.getStatus_time();
            ZonedDateTime zdt = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
            long date = zdt.toInstant().toEpochMilli();
            item.setStatusTime(date);
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

}
