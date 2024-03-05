package searchengine.dto.search;
import lombok.Data;


import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private Integer count;
    private List<searchengine.dto.search.Data> data;
    private String error;
}
