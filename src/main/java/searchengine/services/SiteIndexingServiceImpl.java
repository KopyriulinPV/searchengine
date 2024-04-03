package searchengine.services;
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
import java.util.concurrent.atomic.AtomicBoolean;
@Service
public class SiteIndexingServiceImpl implements SiteIndexingService {
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private final SitesList sites;
    public static AtomicBoolean indexingIsGone;
    private LemmaFinder lemmaFinder;
    @Autowired
    public SiteIndexingServiceImpl(PageRepository pageRepository, SiteRepository siteRepository,
                                   LemmaRepository lemmaRepository, IndexRepository indexRepository,
                                   SitesList sites, LemmaFinder lemmaFinder, AtomicBoolean indexingIsGone) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sites = sites;
        this.lemmaFinder = lemmaFinder;
        this.indexingIsGone = indexingIsGone;
    }
    /**
     * ответ на запрос старт индексации
     */
    public IndexingStartResponse indexingStart() throws IOException, InterruptedException {
        IndexingStartResponse indexingStartResponse = new IndexingStartResponse();
        if (indexingIsGone.getOpaque() == false) {
            for (searchengine.config.Site site : sites.getSites()) {
                Iterable<searchengine.model.Site> iterable = siteRepository.findAll();
                Iterator<searchengine.model.Site> siteIterator = iterable.iterator();
                while (siteIterator.hasNext()) {
                    searchengine.model.Site iteratorNext = siteIterator.next();
                    if (iteratorNext.getUrl().contains(site.getUrl())) {
                        siteRepository.delete(iteratorNext);
                    }
                }
            }
            indexingIsGone.set(true);
            TravelingTheWeb.indexingStop = false;
            indexingStartResponse.setResult(true);
            siteIndexing();
        } else {
            indexingStartResponse.setResult(false);
            indexingStartResponse.setError("Индексация уже запущена");
        }
        return indexingStartResponse;
    }
    /**
     * ответ на запрос остановка индексации
     */
    public IndexingStopResponse indexingStop() {
        IndexingStopResponse indexingStopResponse = new IndexingStopResponse();
        if (indexingIsGone.getOpaque() == false) {
            indexingStopResponse.setResult(false);
            indexingStopResponse.setError("Индексация не запущена");
        } else {
            TravelingTheWeb.indexingStop = true;
            indexingIsGone.set(false);
            indexingStopResponse.setResult(true);
        }
        return indexingStopResponse;
    }
    /**
     * запуск алгоритма индексации, запуск ForkJoinPool по обходу сайта
     */
    public void siteIndexing()
            throws IOException, InterruptedException {
        for (searchengine.config.Site site : sites.getSites()) {
            new Thread(()-> {
                Site newSite = new Site();
                newSite.setName(site.getName());
                newSite.setUrl(site.getUrl());

                newSite.setStatus(Status.INDEXING);
                newSite.setStatus_time(LocalDateTime.now());
                siteRepository.saveAndFlush(newSite);
                TravelingTheWeb action = new TravelingTheWeb(newSite, pageRepository,
                        siteRepository, site.getUrl(), lemmaRepository, indexRepository, lemmaFinder);

                ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
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
            newSite.setStatus(Status.INDEXED);
            siteRepository.saveAndFlush(newSite);
        }
    }
}

