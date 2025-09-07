package searchengine.services;

import searchengine.dto.GeneralResponse;

public interface IndexService {
    GeneralResponse startIndexing();

    GeneralResponse stopIndexing();

    GeneralResponse indexPage(String url);
}