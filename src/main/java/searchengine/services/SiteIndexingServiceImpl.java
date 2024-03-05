package searchengine.services;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@Data
public class SiteIndexingServiceImpl implements SiteIndexingService {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    private final SitesList sites;
    public static boolean indexingIsGone = false;
    private IndexingStartResponse indexingStartResponse;
    private IndexingStopResponse indexingStopResponse;
    private LemmaFinder lemmaFinder;
    @Autowired
    public SiteIndexingServiceImpl(SitesList sites, IndexingStartResponse indexingStartResponse,
                                   IndexingStopResponse indexingStopResponse, LemmaFinder lemmaFinder) {
        this.sites = sites;
        this.indexingStartResponse = indexingStartResponse;
        this.indexingStopResponse = indexingStopResponse;
        this.lemmaFinder = lemmaFinder;
    }
    /**
     * ответ на запрос старт индексации
     */
    public IndexingStartResponse indexingStartResponse() {
        if (indexingIsGone == true) {
            indexingStartResponse.setResult(false);
            indexingStartResponse.setError("Индексация уже запущена");
            return indexingStartResponse;
        }
        indexingIsGone = true;
        TravelingTheWeb.indexingStop = false;
        indexingStartResponse.setResult(true);
        return indexingStartResponse;
    }
    /**
     * ответ на запрос остановка индексации
     */
    public IndexingStopResponse indexingStopResponse() {

        if (indexingIsGone == false) {
            indexingStopResponse.setResult(false);
            indexingStopResponse.setError("Индексация не запущена");
            return indexingStopResponse;
        }
        TravelingTheWeb.indexingStop = true;
        indexingIsGone = false;
        indexingStopResponse.setResult(true);
        return indexingStopResponse;
    }
    /**
     * запуск алгоритма индексации, запуск ForkJoinPool по обходу сайта
     */
    public void siteIndexing(SiteRepository siteRepository, PageRepository pageRepository,
                             LemmaRepository lemmaRepository, IndexRepository indexRepository)
            throws IOException, InterruptedException {
        for (searchengine.config.Site site : sites.getSites()) {
            if (indexingIsGone == false) {
                break;
            }
            Iterable<searchengine.model.Site> iterable = siteRepository.findAll();
            Iterator<searchengine.model.Site> siteIterator = iterable.iterator();
            while (siteIterator.hasNext()) {
                searchengine.model.Site iteratorNext = siteIterator.next();
                if (iteratorNext.getUrl().equals(site.getUrl())) {
                    siteRepository.delete(iteratorNext);
                }
            }
            new Thread(()-> {
                Site newSite = new Site();
                newSite.setName(site.getName());
                newSite.setUrl(site.getUrl());

                newSite.setStatus(Status.INDEXING);
                newSite.setStatus_time(LocalDateTime.now());
                siteRepository.saveAndFlush(newSite);

                TravelingTheWeb action = new TravelingTheWeb(newSite, pageRepository,
                        siteRepository, site.getUrl(), lemmaRepository, indexRepository, lemmaFinder);
                ForkJoinPool pool = new ForkJoinPool(4);
                String listPath = pool.invoke(action);
                indexingCompletionMethod(pool, listPath, newSite);
            }).start();
        }
    }
    /**
     * метод завершения индексации
     */
    private void indexingCompletionMethod(ForkJoinPool pool, String listPath, Site newSite) {
        if (listPath.matches("Индексация остановлена пользователем")) {
            pool.shutdown();
            if (!(newSite.getStatus().compareTo(Status.INDEXED) == 0)) {
                newSite.setStatus(Status.FAILED);
                newSite.setLast_error("Индексация остановлена пользователем");
                siteRepository.saveAndFlush(newSite);
            }
        }
        if (listPath.matches("Индексация завершена")) {
            System.out.println("Индексация завершена - это ошибка");
            newSite.setStatus(Status.INDEXED);
            siteRepository.saveAndFlush(newSite);
        }
    }
}
