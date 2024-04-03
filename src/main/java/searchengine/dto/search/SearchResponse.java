package searchengine.dto.search;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SearchResponse {
    private boolean result;
    private Integer count;
    private List<searchengine.dto.search.FoundPageData> data;
    private String error;
}
