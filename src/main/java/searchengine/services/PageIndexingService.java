package searchengine.services;
import searchengine.dto.IndexingPage.IndexingPageResponse;
import java.io.IOException;

public interface PageIndexingService {
    IndexingPageResponse indexPage(String url) throws IOException, InterruptedException;
}
