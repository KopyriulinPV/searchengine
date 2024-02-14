package searchengine.services;
import lombok.Data;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
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
@Data
public class TravelingTheWeb extends RecursiveTask<String> {
    private List<String> linksFromTags = new ArrayList<>();
    private Site site;
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private String url;
    public static boolean indexingStop;
    public TravelingTheWeb(Site site, PageRepository pageRepository,
                           SiteRepository siteRepository, String Url,
                           LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.url = Url;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }
    public TravelingTheWeb() {
    }
    String regex = "http[s]?://[^#, \\s]*\\.?[a-z]*\\.ru[^#,\\s]*";
    public int i = 0;
    @Override
    protected String compute() {
        try {

            Document document = Jsoup.connect(url).get();
            Thread.sleep(500);
            Elements elements = document.select("body").select("a");
            elements.forEach(element -> {
                linksFromTags.add(element.absUrl("href"));
            });

            for (int j = 0; j < linksFromTags.size(); j++) {

                if (linksFromTags.get(j).matches(regex)) {
                    URL pathUrl = new URL(linksFromTags.get(j));
                    URL pathUrl2 = new URL(url);

                    Pattern pattern = Pattern.compile(pathUrl2.getHost());
                    Matcher matcher = pattern.matcher(linksFromTags.get(j));

                    if (indexingStop == true) {
                        return "Индексация остановлена пользователем";
                    }

                    List<Page> pagesList = pageRepository.findByPath(pathUrl.getPath());
                    if ((pagesList.size() == 0) && matcher.find()) {
                        indexingSitePage(linksFromTags.get(j), pathUrl);
                    }
                    List<Page> pagesList1 = pageRepository.findByPath(pathUrl.getPath());
                    List<Page> pagesList2 = pageRepository.findByPath(pathUrl.getPath());

                    for (Page newPage : pagesList1) {
                        if (newPage.getSite().getUrl().contains(pathUrl.getHost())) {
                            pagesList2.add(newPage);
                        }
                    }
                    if (pagesList2.size() == 0 && matcher.find()) {
                        indexingSitePage(linksFromTags.get(j), pathUrl);
                    }
                 }
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            String error = ex.toString();
            return error;
        }

        return null;
    }

    private void indexingSitePage(String nexUrl, URL pathUrl) throws IOException {

        TravelingTheWeb action = new TravelingTheWeb(site, pageRepository,
                siteRepository, nexUrl, lemmaRepository, indexRepository);
        action.fork();
        site.setStatus_time(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
        Page pageEntity = new Page();
        pageEntity.setPath(pathUrl.getPath());
        pageEntity.setSite(site);
        Connection.Response code = Jsoup.connect(nexUrl).execute();
        pageEntity.setCode(code.statusCode());

        Document document1 = Jsoup.connect(nexUrl).get();

        pageEntity.setContent(document1.getAllElements().toString());

        pageRepository.saveAndFlush(pageEntity);
        indexingLemmaIndex(nexUrl, pageEntity);
        System.out.println(Thread.currentThread().getName() +
                "        " + pathUrl + "   " + pageEntity.getPath());
        action.join();
    }

    private void indexingLemmaIndex(String nexUrl, Page pageEntity) throws IOException {
        LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
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
            for (int q = 0; q < lemmaList.size(); q++) {
                if (lemmaList.get(q).getLemma().equals(lemmaString)) {
                    lemmaList1.add(lemmaList.get(q));
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
                for (int i = 0; i < lemmaList1.size(); i++) {
                    if (lemmaList1.get(i).getLemma().equals(lemmaString) &&
                            (lemmaList1.get(i).getSite().getId() == site.getId())) {
                        lemmaList1.get(i).setFrequency(lemmaList1.get(i).getFrequency() + 1);
                        lemmaRepository.saveAndFlush(lemmaList1.get(i));

                        Index index = new Index();
                        index.setPage(pageEntity);
                        index.setLemma(lemmaList1.get(i));
                        index.setLemma_rank(entry.getValue());
                        indexRepository.saveAndFlush(index);
                        break;
                    }
                }
            }
        }
    }
}











