package searchengine.services;

import java.io.IOException;
import java.net.MalformedURLException;

public interface PageIndexingService {

    void pageIndexing(String url, searchengine.config.Site site) throws IOException, InterruptedException;
}
