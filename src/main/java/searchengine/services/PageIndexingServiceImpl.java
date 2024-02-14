package searchengine.services;

import lombok.Data;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

@Data
@Service
public class PageIndexingServiceImpl implements PageIndexingService {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    public void pageIndexing(String url, searchengine.config.Site site1) throws IOException, InterruptedException {
        URL url2 = new URL(url);
        List<Site> sites1 = siteRepository.findAllContains(url2.getHost());
        if (sites1.size() == 0) {
           Site site2 = new Site();
           site2.setStatus(Status.INDEXING);
           site2.setStatus_time(LocalDateTime.now());
           site2.setUrl(site1.getUrl());
           site2.setName(site1.getName());
           siteRepository.saveAndFlush(site2);
        }
        List<Site> sites = siteRepository.findAllContains(url2.getHost());
        Site site = sites.get(0);

        Page page = new Page();
        page.setSite(site);
        URL url1 = new URL(url);
        page.setPath(url1.getPath());
        Connection.Response code = Jsoup.connect(url).execute();
        page.setCode(code.statusCode());
        Document document = Jsoup.connect(url).get();
        page.setContent(document.getAllElements().toString());
        pageRepository.saveAndFlush(page);

        LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
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
            for (int j=0; j < lemmaList.size(); j++){
                if (lemmaList.get(j).getLemma().equals(lemmaString)) {
                    lemmaList1.add(lemmaList.get(j));
                }
            }

            if (lemmaList.size()==0) {
                lemmaRepository.saveAndFlush(lemma);

                Index index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setLemma_rank(entry.getValue());
                indexRepository.saveAndFlush(index);

            } else {
                for (int i=0; i < lemmaList1.size(); i++){
                    if (lemmaList1.get(i).getLemma().equals(lemmaString)&&
                            lemmaList1.get(i).getSite().getId()==site.getId()) {
                        lemmaList1.get(i).setFrequency(lemmaList1.get(i).getFrequency() + 1);
                        lemmaRepository.saveAndFlush(lemmaList1.get(i));

                        Index index = new Index();
                        index.setPage(page);
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
