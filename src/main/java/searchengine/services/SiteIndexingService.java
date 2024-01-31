package searchengine.services;


import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;

public interface SiteIndexingService {
    void siteIndexing(SiteRepository siteRepository, PageRepository pageRepository, Site site) throws IOException, InterruptedException;


}
