package searchengine.dto.Indexing;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class IndexingStopResponse {
    private boolean result;
    private String error;
}
