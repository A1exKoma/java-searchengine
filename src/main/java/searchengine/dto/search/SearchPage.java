package searchengine.dto.search;

import lombok.Data;
import searchengine.model.PageModel;

@Data
public class SearchPage {
    public SearchPage(PageModel pageModel, float rank) {
        this.pageModel = pageModel;
        this.rank = rank;
    }

    PageModel pageModel;
    float rank;
}
