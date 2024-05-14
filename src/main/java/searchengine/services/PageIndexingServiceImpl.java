package searchengine.services;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.IndexingPage.IndexingPageResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PageIndexingServiceImpl implements PageIndexingService {
    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private final SitesList sites;
    private LemmaFinder lemmaFinder;

    @Autowired
    public PageIndexingServiceImpl(PageRepository pageRepository, SiteRepository siteRepository,
                                   LemmaRepository lemmaRepository, IndexRepository indexRepository,
                                   SitesList sites, LemmaFinder lemmaFinder) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sites = sites;
        this.lemmaFinder = lemmaFinder;
    }

    public IndexingPageResponse indexPage(String url) throws IOException, InterruptedException {
        URL urlIndexingPage = createNormalURLForm(url);
        List<Page> pagesWithPathIndexingPage = pageRepository.findByPath(urlIndexingPage.getPath());
        for (searchengine.config.Site site : sites.getSites()) {
            IndexingPageResponse indexingPageResponse = new IndexingPageResponse();
            createResponseToPageIndexing(urlIndexingPage, pagesWithPathIndexingPage, site, indexingPageResponse);
            if (indexingPageResponse.isResult()) {
                return indexingPageResponse;
            }
        }
        IndexingPageResponse indexingPageResponse2 = new IndexingPageResponse();
        indexingPageResponse2.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        return indexingPageResponse2;
    }

    private void createResponseToPageIndexing(URL urlIndexingPage, List<Page> pagesWithPathIndexingPage,
                                              searchengine.config.Site site, IndexingPageResponse indexingPageResponse) throws IOException {
        Pattern pattern1 = Pattern.compile(urlIndexingPage.getHost());
        Matcher coincidenceSiteOfConfigAndUrlIndexingPage = pattern1.matcher(site.getUrl());

        for (Page page : pagesWithPathIndexingPage) {
            if ((coincidenceSiteOfConfigAndUrlIndexingPage.find()) &&
                    (page.getPath().equals(urlIndexingPage.getPath()))
                    && (page.getSite().getUrl().contains(urlIndexingPage.getHost()))) {
                List<Index> indexes = indexRepository.findByPage_id(page.getId());
                saveLemma(indexes);
                pageRepository.delete(page);
                saveSitePage(urlIndexingPage, site);
                indexingPageResponse.setResult(true);
            }
        }
        if ((coincidenceSiteOfConfigAndUrlIndexingPage.find()) && (pagesWithPathIndexingPage.size() == 0)) {
            saveSitePage(urlIndexingPage, site);
            indexingPageResponse.setResult(true);
        }
        for (Page page : pagesWithPathIndexingPage) {
            if ((coincidenceSiteOfConfigAndUrlIndexingPage.find()) &&
                    (page.getPath().equals(urlIndexingPage.getPath()))
                    && !(page.getSite().getUrl().contains(urlIndexingPage.getHost()))) {
                saveSitePage(urlIndexingPage, site);
                indexingPageResponse.setResult(true);
            }
        }
    }

    private void saveLemma(List<Index> indexes) {
        for (Index index : indexes) {
            Lemma lemmaNew = lemmaRepository.findById(index.getLemma().getId()).get();
            lemmaNew.setFrequency(lemmaNew.getFrequency() - 1);
            lemmaRepository.saveAndFlush(lemmaNew);
        }
    }

    private URL createNormalURLForm(String url) throws MalformedURLException {
        String regex = "url=";
        String regex1 = "%3A%2F%2F";
        String regex2 = "%2F";
        String stringUrl = url.replace(regex, "").replace(regex1, "://").replace(regex2, "/");
        URL urlUrl = new URL(stringUrl);
        return urlUrl;
    }

    private void saveSitePage(URL urlIndexingPage, searchengine.config.Site siteConfig) throws IOException {
        List<Site> sitesBeforeRecording = siteRepository.findAllContains(urlIndexingPage.getHost());
        if (sitesBeforeRecording.size() == 0) {
            Site site2 = new Site();
            site2.setStatus(Status.INDEXING);
            site2.setStatus_time(LocalDateTime.now());
            site2.setUrl(siteConfig.getUrl());
            site2.setName(siteConfig.getName());
            siteRepository.saveAndFlush(site2);
        }
        List<Site> sitesAfterRecording = siteRepository.findAllContains(urlIndexingPage.getHost());
        Site site = sitesAfterRecording.get(0);
        Page page = new Page();
        page.setSite(site);
        page.setPath(urlIndexingPage.getPath());
        Connection.Response code = Jsoup.connect(urlIndexingPage.toString()).execute();
        page.setCode(code.statusCode());
        Document document = Jsoup.connect(urlIndexingPage.toString()).get();
        page.setContent(document.getAllElements().toString());
        pageRepository.saveAndFlush(page);
        saveLemmaIndex(document, site, page);
    }

    private void saveLemmaIndex(Document document, Site site, Page page) {
        Map<String, Integer> lemmaFinderMap = lemmaFinder.collectLemmas(document.
                getAllElements().toString());
        for (Map.Entry<String, Integer> entry : lemmaFinderMap.entrySet()) {
            String lemmaString = entry.getKey();
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(lemmaString);
            lemma.setFrequency(1);
            List<Lemma> lemmaList = lemmaRepository.findAllContains(lemmaString, site.getId());
            List<Lemma> lemmaList1 = new ArrayList<>();
            addLemmaInLemmaList(lemmaList, lemmaList1, lemmaString);
            if (lemmaList.size() == 0) {
                lemmaRepository.saveAndFlush(lemma);
                Index index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setLemma_rank(entry.getValue());
                indexRepository.saveAndFlush(index);
            } else {
                saveLemmaIndexIfLemmaListNoEmpty(lemmaList1, lemmaString, site, page, entry);
            }
        }
    }

    private void saveLemmaIndexIfLemmaListNoEmpty(List<Lemma> lemmaList1, String lemmaString,
                                                  Site site, Page page, Map.Entry<String, Integer> entry) {
        for (Lemma lemma1 : lemmaList1) {
            if (lemma1.getLemma().equals(lemmaString) &&
                    lemma1.getSite().getId() == site.getId()) {
                lemma1.setFrequency(lemma1.getFrequency() + 1);
                lemmaRepository.saveAndFlush(lemma1);
                Index index = new Index();
                index.setPage(page);
                index.setLemma(lemma1);
                index.setLemma_rank(entry.getValue());
                indexRepository.saveAndFlush(index);
                break;
            }
        }
    }

    private void addLemmaInLemmaList(List<Lemma> lemmaList, List<Lemma> lemmaList1, String lemmaString) {
        for (Lemma lemma1 : lemmaList) {
            if (lemma1.getLemma().equals(lemmaString)) {
                lemmaList1.add(lemma1);
            }
        }
    }
}

