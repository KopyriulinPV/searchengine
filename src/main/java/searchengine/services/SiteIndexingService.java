package searchengine.services;
import searchengine.dto.Indexing.IndexingStartResponse;
import searchengine.dto.Indexing.IndexingStopResponse;
import java.io.IOException;

public interface SiteIndexingService {
    void siteIndexing() throws IOException, InterruptedException;

    IndexingStartResponse indexingStartResponse() throws IOException, InterruptedException;

    IndexingStopResponse indexingStopResponse();
}
