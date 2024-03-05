package searchengine.dto.search;

@lombok.Data
public class Data {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    Float relevance;
}
