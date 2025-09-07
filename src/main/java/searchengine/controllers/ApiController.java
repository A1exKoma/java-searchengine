package searchengine.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.GeneralResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexService indexService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexService indexService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexService = indexService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public GeneralResponse startIndexing() {
        return indexService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public GeneralResponse stopIndexing() {
        return indexService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public GeneralResponse indexPage(String url) {
        return indexService.indexPage(url);
    }

    @GetMapping("/search")
    public Object search(String query, String site, int offset, int limit) {
        return searchService.search(query, site, offset, limit);
    }
}