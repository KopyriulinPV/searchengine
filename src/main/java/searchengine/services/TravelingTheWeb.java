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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            System.out.println(site.getUrl() + " " + Thread.currentThread());
             for (String tag : formationListUrl(url)) {
                if (tag.matches(regex)) {
                    URL pathUrl = new URL(tag);
                    URL pathUrl2 = new URL(url);
                    Pattern pattern = Pattern.compile(pathUrl2.getHost());
                    Matcher matcher = pattern.matcher(tag);
                    List<Page> pagesList = pageRepository.findByPath(pathUrl.getPath());
                    if ((pagesList.size() == 0) && matcher.find()) {
                        String indexingStop = indexingSitePage(tag, pathUrl);
                        if (indexingStop.matches("Индексация остановлена пользователем")) {
                            return "Индексация остановлена пользователем";
                        }
                    }
                    List<Page> pagesList1 = pageRepository.findByPath(pathUrl.getPath());
                    List<Page> pagesList2 = pageRepository.findByPath(pathUrl.getPath());
                    for (Page newPage : pagesList1) {
                        if (newPage.getSite().getUrl().contains(pathUrl2.getHost())) {
                            pagesList2.add(newPage);
                        }
                    }
                    if (pagesList2.size() == 0 && matcher.find()) {
                        String indexingStop = indexingSitePage(tag, pathUrl);
                        if (indexingStop.matches("Индексация остановлена пользователем")) {
                            return "Индексация остановлена пользователем";
                        }
                    }
                    if (indexingStop == true) {
                        return "Индексация остановлена пользователем";
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
        site.setStatus_time(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
        Page pageEntity = new Page();
        pageEntity.setPath(pathUrl.getPath());
        pageEntity.setSite(site);
        Connection.Response code = Jsoup.connect(nexUrl).execute();
        pageEntity.setCode(code.statusCode());

        Document document1 = Jsoup.connect(nexUrl).get();
        Thread.sleep(1000);
        pageEntity.setContent(document1.getAllElements().toString());
        pageRepository.saveAndFlush(pageEntity);
        String indexingStop = indexingLemmaIndex(nexUrl, pageEntity);
        if (indexingStop.matches("Индексация остановлена пользователем")) {
            return "Индексация остановлена пользователем";
        }
        action.join();
        return "";
    }
    /**
     * запись в БД проиндексированных сущностей Lemma и Index
     */
    private String indexingLemmaIndex(String nexUrl, Page pageEntity) throws IOException {
        if (indexingStop == true) {
            return "Индексация остановлена пользователем";
        }
        Document document2 = Jsoup.connect(nexUrl).get();
        Map<String, Integer> lemmaFinderMap = lemmaFinder.collectLemmas(document2.
                getAllElements().toString());
        for (Map.Entry<String, Integer> entry : lemmaFinderMap.entrySet()) {
            String lemmaString = entry.getKey();
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(lemmaString);
            lemma.setFrequency(1);
            List<Lemma> lemmaList = lemmaRepository.findAllContains(lemmaString, site.getId());
            List<Lemma> lemmaList1 = new ArrayList<>();
            for (Lemma lemma1 : lemmaList) {
                if (lemma1.getLemma().equals(lemmaString)) {
                    lemmaList1.add(lemma1);
                }
            }
            if (lemmaList.size() == 0) {
                lemmaRepository.saveAndFlush(lemma);
                Index index = new Index();
                index.setPage(pageEntity);
                index.setLemma(lemma);
                index.setLemma_rank(entry.getValue());
                indexRepository.saveAndFlush(index);
            } else {
                for (Lemma lemma1 : lemmaList1) {
                    if (lemma1.getLemma().equals(lemmaString) &&
                            (lemma1.getSite().getId() == site.getId())) {
                        lemma1.setFrequency(lemma1.getFrequency() + 1);
                        lemmaRepository.saveAndFlush(lemma1);
                        Index index = new Index();
                        index.setPage(pageEntity);
                        index.setLemma(lemma1);
                        index.setLemma_rank(entry.getValue());
                        indexRepository.saveAndFlush(index);
                        break;
                    }
                }
            }
        }
        return "";
    }
    /**
     * формирование листа url
     */
    private List<String> formationListUrl(String url) throws InterruptedException, IOException {
    List<String> linksFromTags = new ArrayList<>();
    Document document = Jsoup.connect(url).get();
            Thread.sleep(1000);
    Elements elements = document.select("body").select("a");
            elements.forEach(element -> {
        linksFromTags.add(element.absUrl("href"));
    });
      return linksFromTags;
    }
}











