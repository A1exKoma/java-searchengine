package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData statistics = new StatisticsData();
        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        List<SiteModel> sites = siteRepository.findAll();
        for (SiteModel site : sites) {
            DetailedStatisticsItem detailedItem = new DetailedStatisticsItem();
            detailedItem.setUrl(site.getUrl());
            detailedItem.setName(site.getName());
            detailedItem.setStatus(site.getStatus().toString());
            detailedItem.setStatusTime(site.getStatusTime().getTime());
            detailedItem.setError(site.getLastError());
            if (detailedItem.getError() == null) {
                detailedItem.setError("");
            }
            detailedItem.setPages(pageRepository.countBySiteId(site.getId()));
            total.setPages(total.getPages() + detailedItem.getPages());
            detailedItem.setLemmas(lemmaRepository.countBySiteId(site.getId()));
            total.setLemmas(total.getLemmas() + detailedItem.getLemmas());
            detailed.add(detailedItem);
        }
        statistics.setDetailed(detailed);

        total.setSites(sites.size());
        total.setIndexing(true);
        statistics.setTotal(total);

        response.setStatistics(statistics);
        response.setResult(true);
        return response;
    }
}