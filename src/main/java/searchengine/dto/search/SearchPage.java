package searchengine.dto.search;

import lombok.Data;
import searchengine.model.PageModel;

@Data
public class SearchPage {
    PageModel pageModel;
    float rank;

    public SearchPage(PageModel pageModel, float rank) {
        this.pageModel = pageModel;
        this.rank = rank;
    }
}
