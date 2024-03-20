package searchengine.dto.Indexing;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class IndexingStartResponse {
    private boolean result;
    private String error;
}
