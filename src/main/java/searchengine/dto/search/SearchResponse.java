package searchengine.dto.search;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResponse {
    boolean result;
    int count;
    List<SearchData> data = new ArrayList<>();

    public void addData(SearchData searchData) {
        data.add(searchData);
    }
}
