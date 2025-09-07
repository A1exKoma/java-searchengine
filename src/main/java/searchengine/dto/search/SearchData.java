package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchData {
    String site;
    String siteName;
    String url;
    String title;
    String snippet;
    double relevance;
}
