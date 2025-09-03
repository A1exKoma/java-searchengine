package searchengine.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.GeneralResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteService siteService;

    public ApiController(StatisticsService statisticsService, SiteService siteService) {
        this.statisticsService = statisticsService;
        this.siteService = siteService;
    }

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics(siteService);
    }

    @GetMapping("/startIndexing")
    public GeneralResponse startIndexing() {
        if (siteService.isIndexing()) {
            return new GeneralResponse(false,"Индексация уже запущена");
        }
        else {
            new Thread(siteService::startIndexing).start();
            return new GeneralResponse(true);
        }
    }

    @GetMapping("/stopIndexing")
    public GeneralResponse stopIndexing() {
        if (siteService.isIndexing()) {
            siteService.stopIndexing();
            return new GeneralResponse(true);
        }
        else return new GeneralResponse(false,"Индексация не запущена");
    }

    @PostMapping("/indexPage")
    public GeneralResponse indexPage(String url) {
        if (siteService.indexPage(url)) return new GeneralResponse(true);
        return new GeneralResponse(false,"Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }

    @GetMapping("/search")
    public Object search(String query, String site) {
        return siteService.search(query, site);
    }
}