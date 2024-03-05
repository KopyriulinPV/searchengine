package searchengine.services;

import searchengine.dto.IndexingPage.IndexingPageResponse;

import java.io.IOException;
public interface PageIndexingService {
    IndexingPageResponse pageIndexing(String url) throws IOException, InterruptedException;
}
