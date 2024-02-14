package searchengine.services;

import searchengine.dto.search.SearchResponse;

import java.io.IOException;
import java.util.List;

public interface SearchServices {
        SearchResponse getSearch(String query, String offset, String limit, String site) throws IOException, InterruptedException;
}
