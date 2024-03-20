package searchengine.dto.search;

@lombok.Data
public class FoundPageData {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;
}
