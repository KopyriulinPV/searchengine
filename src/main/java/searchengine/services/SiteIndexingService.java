package searchengine.services;


import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;

public interface SiteIndexingService {
    void siteIndexing(SiteRepository siteRepository, PageRepository pageRepository,
                      LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException, InterruptedException;

    IndexingStartResponse indexingStartResponse();

    IndexingStopResponse indexingStopResponse();
}
