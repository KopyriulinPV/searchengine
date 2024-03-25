package searchengine.services;
import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;
import java.io.IOException;

public interface SiteIndexingService {
    void siteIndexing() throws IOException, InterruptedException;

    IndexingStartResponse indexingStart() throws IOException, InterruptedException;

    IndexingStopResponse indexingStop();
}
