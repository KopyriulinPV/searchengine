package searchengine.services;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
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

@Getter
@Setter
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    TotalStatistics total;
    StatisticsResponse response;
    StatisticsData data;
    DetailedStatisticsItem item;

    @Autowired
    public StatisticsServiceImpl(TotalStatistics total, StatisticsResponse response, StatisticsData data) {
        this.total = total;
        this.response = response;
        this.data = data;
    }
    /**
     * запуск формирования данных по статистике
     */
    @Override
    public StatisticsResponse getStatistics() throws SQLException {
        List<searchengine.model.Site> sitesList = siteRepository.findAll();
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
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(formationDetailedStatisticsItem(item, site.getUrl(), site.getName(), site.getStatus().toString(),
                    date, site.getLast_error(), pages, lemmas));
        }
        return formationStatisticsResponse(total, detailed);
    }
    /**
     * заполнение объекта DetailedStatisticsItem
     */
     private DetailedStatisticsItem formationDetailedStatisticsItem(DetailedStatisticsItem item , String url,
                                                                    String name, String status,
                                                                   long statusTime, String error, int pages, int lemmas) {
        item.setUrl(url);
        item.setName(name);
        item.setStatus(status);
        item.setStatusTime(statusTime);
        item.setError(error);
        item.setPages(pages);
        item.setLemmas(lemmas);
        return item;
    }
     /**
     * заполнение объекта StatisticsResponse
     */
     private StatisticsResponse formationStatisticsResponse(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
