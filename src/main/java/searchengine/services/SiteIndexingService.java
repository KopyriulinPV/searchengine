package searchengine.services;


import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;

public interface SiteIndexingService {
    void siteIndexing(SiteRepository siteRepository, PageRepository pageRepository, Site site,
                      LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException, InterruptedException;


}
