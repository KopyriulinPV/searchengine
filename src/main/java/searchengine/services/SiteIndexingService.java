package searchengine.services;

import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;

import java.io.IOException;

public interface SiteIndexingService {
    void indexSites() throws IOException, InterruptedException;

    IndexingStartResponse startOfIndexing() throws IOException, InterruptedException;

    IndexingStopResponse stopIndexing();
}
