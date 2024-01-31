package searchengine.services;

import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.model.Status;
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

    public void siteIndexing(SiteRepository siteRepository, PageRepository pageRepository, Site site) throws IOException, InterruptedException {

        site.setStatus(Status.INDEXING);
        site.setStatus_time(LocalDateTime.now());
        siteRepository.saveAndFlush(site);

        TravelingTheWeb action = new TravelingTheWeb(site, pageRepository,
                siteRepository, site.getUrl());
        ForkJoinPool pool = new ForkJoinPool(4);
        String listPath = pool.invoke(action);

        if (listPath == null) {
            site.setLast_error(listPath);
            site.setStatus(Status.INDEXED);
        } else {
            site.setLast_error(listPath);
            site.setStatus(Status.FAILED);
        }
        siteRepository.saveAndFlush(site);
    }





}
