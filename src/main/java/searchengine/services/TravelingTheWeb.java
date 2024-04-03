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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import searchengine.repositories.SiteRepository;


@Service
@Setter
@Getter
public class TravelingTheWeb extends RecursiveTask<String> {
    private Site site;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    private String url;
    public static boolean indexingStop;
    private LemmaFinder lemmaFinder;

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
            Thread.sleep(1000);
            System.out.println(site.getUrl() + " " + Thread.currentThread());
            for (String tag : formationListUrl(url, document)) {
                if (tag.matches(regex)) {
                    URL pathUrl = new URL(tag);
                    URL pathUrl2 = new URL(url);
                    Pattern pattern = Pattern.compile(pathUrl2.getHost());
                    Matcher matcher = pattern.matcher(tag);
                    List<Page> pagesList = pageRepository.findByPath(pathUrl.getPath());
                    if ((pagesList.size() == 0) && matcher.find()) {
                        doStatusIndexing();
                        String indexingStop = indexingSitePage(tag, pathUrl);
                        if (indexingStop.matches("Индексация остановлена пользователем")) {
                            return "Индексация остановлена пользователем";
                        }
                        return "Индексация завершена";
                    }
                    List<Page> pagesList1 = pageRepository.findByPath(pathUrl.getPath());
                    List<Page> pagesList2 = pageRepository.findByPath(pathUrl.getPath());
                    for (Page newPage : pagesList1) {
                        if (newPage.getSite().getUrl().contains(pathUrl2.getHost())) {
                            pagesList2.add(newPage);
                        }
                    }
                    if (pagesList2.size() == 0 && matcher.find()) {
                        doStatusIndexing();
                        String indexingStop = indexingSitePage(tag, pathUrl);
                        if (indexingStop.matches("Индексация остановлена пользователем")) {
                            return "Индексация остановлена пользователем";
                        }
                        return "Индексация завершена";
                    }
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

    /**
     * запись в БД проиндексированных сущностей Site и Page
     */
    private String indexingSitePage(String nexUrl, URL pathUrl) throws IOException, InterruptedException {
        if (indexingStop == true) {
            return "Индексация остановлена пользователем";
        }
        TravelingTheWeb action = new TravelingTheWeb(site, pageRepository,
                siteRepository, nexUrl, lemmaRepository, indexRepository, lemmaFinder);
        action.fork();
        Page pageEntity = new Page();
        pageEntity.setPath(pathUrl.getPath());
        pageEntity.setSite(site);
        Thread.sleep(1000);
        Document document1 = Jsoup.connect(nexUrl).get();
        Thread.sleep(1000);

        Connection.Response code = Jsoup.connect(nexUrl).execute();
        pageEntity.setCode(code.statusCode());

        pageEntity.setContent(document1.getAllElements().toString());
        pageRepository.saveAndFlush(pageEntity);
        String indexingStop = indexingLemmaIndex(pageEntity, document1);
        if (indexingStop.matches("Индексация остановлена пользователем")) {
            return "Индексация остановлена пользователем";
        }
        action.join();
        return "";
    }

    /**
     * запись в БД проиндексированных сущностей Lemma и Index
     */
    private String indexingLemmaIndex(Page pageEntity, Document document) throws IOException {
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

    /**
     * формирование листа url
     */
    private List<String> formationListUrl(String url, Document document) throws InterruptedException, IOException {
        List<String> linksFromTags = new ArrayList<>();
        Elements elements = document.select("body").select("a");
        elements.forEach(element -> {
            linksFromTags.add(element.absUrl("href"));
        });
        return linksFromTags;
    }
    private void doStatusIndexing() {
        site.setStatus(Status.INDEXING);
        site.setStatus_time(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
    }
    private void doSaveIndex(Page pageEntity, Lemma lemma, Integer lemma_rank, Set<Index> listIndex) {
        Index index = new Index();
        index.setPage(pageEntity);
        index.setLemma(lemma);
        index.setLemma_rank(lemma_rank);
        listIndex.add(index);
    }
}











