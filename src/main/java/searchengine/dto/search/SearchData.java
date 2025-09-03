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

    public String toString() {
        return siteName + " " + site + url + " : " + relevance + "\n" + title;
    }
}
