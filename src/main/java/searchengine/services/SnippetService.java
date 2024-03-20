package searchengine.services;
import java.io.IOException;

public interface SnippetService {
    String getSnippet(String query, String stringDocument) throws IOException;
}
