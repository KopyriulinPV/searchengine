package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveTask;

import searchengine.repositories.SiteRepository;


@Service
@Setter
@Getter
public class TravelingTheWeb extends RecursiveTask<String> {
    private Site site;
    @Autowired
    private static PageRepository pageRepository;
    @Autowired
    private static SiteRepository siteRepository;
    @Autowired
    private static LemmaRepository lemmaRepository;
    @Autowired
    private static IndexRepository indexRepository;
    private String url;
    public static boolean indexingStop;
    private static LemmaFinder lemmaFinder;

    public TravelingTheWeb(Site site, PageRepository pageRepository,
                           SiteRepository siteRepository, String url,
                           LemmaRepository lemmaRepository, IndexRepository indexRepository, LemmaFinder lemmaFinder) {
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.url = url;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinder = lemmaFinder;
    }

    public TravelingTheWeb() {
    }

    String regex = "http[s]?://[^#, \\s]*\\.?[a-z]*\\.ru[^#,\\s]*";

    @Override
    protected String compute() {
        try {
            Document document = Jsoup.connect(url).get();
            Thread.sleep(500);
            Set<String> tagList = getSetUrl(document);
            document = null;
            for (String tag : tagList) {
                if (tag.matches(regex)) {
                    URL urlSiteIndexing = new URL(site.getUrl());
                    URL urlTag = new URL(tag);
                    URL urlUrl = new URL(url);
                    if (checkForAvailabilityPage(urlUrl, urlTag)) {
                        continue;
                    }
                    if (!urlTag.getHost().equals(urlSiteIndexing.getHost())) {
                        continue;
                    }
                    TravelingTheWeb action = new TravelingTheWeb(site, pageRepository,
                            siteRepository, tag, lemmaRepository, indexRepository, lemmaFinder);
                    action.fork();
                    String resultIndexing = startSaveIfPageRepositoryNotFindByPath(urlUrl, tag, urlTag, action, site);
                    if (resultIndexing.equals("Индексация остановлена пользователем")) {
                        return "Индексация остановлена пользователем";
                    }
                    action.join();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String error = ex.toString();
            site.setLast_error(error);
            site.setStatus(Status.FAILED);
            siteRepository.saveAndFlush(site);
        }
        return "Индексация завершена";
    }

    private static synchronized boolean checkForAvailabilityPage(URL urlUrl, URL urlTag) {
        List<Page> pagesList = getListPageAvailableInTheDatabase(urlUrl, urlTag);
        if (pagesList.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    private static synchronized String startSaveIfPageRepositoryNotFindByPath(URL urlUrl, String tag,
                                                                              URL urlTag, TravelingTheWeb action, Site site)
            throws IOException, InterruptedException {
        List<Page> pagesList = getListPageAvailableInTheDatabase(urlUrl, urlTag);
        if (pagesList.size() == 0) {
            System.out.println(tag + " - " + Thread.currentThread());
            doStatusIndexing(site);
            String indexingStop = saveSitePage(tag, urlTag, action, site);
            if (indexingStop.matches("Индексация остановлена пользователем")) {
                return "Индексация остановлена пользователем";
            }
        }
        return "";
    }

    private static synchronized List<Page> getListPageAvailableInTheDatabase(URL urlUrl, URL urlTag) {
        List<Page> pagesList1 = pageRepository.findByPath(urlTag.getPath());
        List<Page> pagesList2 = new ArrayList<>();
        for (Page newPage : pagesList1) {
            if (newPage.getSite().getUrl().contains(urlUrl.getHost())) {
                pagesList2.add(newPage);
            }
        }
        return pagesList2;
    }

    private static String saveSitePage(String nexUrl, URL pathUrl, TravelingTheWeb action, Site site) throws IOException, InterruptedException {
        if (indexingStop == true) {
            return "Индексация остановлена пользователем";
        }
        Page pageEntity = new Page();
        pageEntity.setPath(pathUrl.getPath());
        pageEntity.setSite(site);

        Document document1 = Jsoup.connect(nexUrl).get();
        Thread.sleep(500);
        Connection.Response code = Jsoup.connect(nexUrl).execute();
        pageEntity.setCode(code.statusCode());
        pageEntity.setContent(document1.getAllElements().toString());
        pageRepository.saveAndFlush(pageEntity);
        String indexingStop = saveLemmaIndex(pageEntity, document1, action, site);
        return indexingStop;
    }

    private static String saveLemmaIndex(Page pageEntity, Document document, TravelingTheWeb action, Site site) throws IOException {
        if (indexingStop == true) {
            return "Индексация остановлена пользователем";
        }
        Map<String, Integer> lemmaFinderMap = lemmaFinder.collectLemmas(document.
                getAllElements().toString());
        Set<Lemma> listLemmaIndexing = new HashSet<>();
        Set<Index> listIndex = new HashSet<>();
        for (Map.Entry<String, Integer> entry : lemmaFinderMap.entrySet()) {
            String lemmaString = entry.getKey();
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(lemmaString);
            lemma.setFrequency(1);
            List<Lemma> lemmaList = lemmaRepository.findAllContains(lemmaString, site.getId());
            List<Lemma> streamListLemma = listLemmaIndexing.stream().filter(r -> r.getLemma().contains(lemmaString)).toList();
            if ((lemmaList.size() == 0) && (streamListLemma.size() == 0)) {
                listLemmaIndexing.add(lemma);
                doSaveIndex(pageEntity, lemma, entry.getValue(), listIndex);

            } else if (!(lemmaList.size() == 0) && (streamListLemma.size() == 0)) {
                Lemma lemma1 = lemmaList.stream().findFirst().get();
                lemma1.setFrequency(lemma1.getFrequency() + 1);
                listLemmaIndexing.add(lemma1);
                doSaveIndex(pageEntity, lemma1, entry.getValue(), listIndex);

            } else {
                Lemma lemma1 = streamListLemma.stream().findFirst().get();
                lemma1.setFrequency(lemma1.getFrequency() + 1);
                listLemmaIndexing.add(lemma1);
                doSaveIndex(pageEntity, lemma1, entry.getValue(), listIndex);
            }
        }
        lemmaRepository.saveAllAndFlush(listLemmaIndexing);
        indexRepository.saveAllAndFlush(listIndex);

        return "";
    }

    private Set<String> getSetUrl(Document document) throws InterruptedException, IOException {
        Set<String> linksFromTags = new HashSet<>();
        Elements elements = document.select("body").select("a");
        elements.forEach(element -> {
            linksFromTags.add(element.absUrl("href"));
        });
        return linksFromTags;
    }

    private static void doStatusIndexing(Site site) {
        site.setStatus(Status.INDEXING);
        site.setStatus_time(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
    }

    private static void doSaveIndex(Page pageEntity, Lemma lemma, Integer lemma_rank, Set<Index> listIndex) {
        Index index = new Index();
        index.setPage(pageEntity);
        index.setLemma(lemma);
        index.setLemma_rank(lemma_rank);
        listIndex.add(index);
    }
}











