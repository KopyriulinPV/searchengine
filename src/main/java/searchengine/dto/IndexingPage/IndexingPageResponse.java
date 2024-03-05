package searchengine.dto.IndexingPage;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
public class IndexingPageResponse {
    private boolean result;
    private String error;
}
