package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.Indexing.IndexingURL;
import searchengine.Indexing.SourceCode;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.GeneralResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    public AtomicBoolean isIndexing = new AtomicBoolean(false);
    public AtomicInteger countIndexingSites = new AtomicInteger(0);

    @Override
    public GeneralResponse startIndexing() {
        if (isIndexing.get()) {
            return new GeneralResponse(false,"Индексация уже запущена");
        }
        isIndexing.set(true);

        for (Site site : sitesList.getSites()) {
            countIndexingSites.incrementAndGet();

            SiteModel siteModel = siteRepository.getByName(site.getName());
            if (siteModel != null) {
                siteRepository.deleteById(siteModel.getId());
            }

            siteModel = new SiteModel();
            siteModel.setName(site.getName());
            siteModel.setUrl(site.getUrl());
            siteModel.setStatus(Status.INDEXING);
            siteModel.setStatusTime(new Date());
            siteRepository.save(siteModel);
            int id = siteModel.getId();

            IndexingURL indexingURL = new IndexingURL("/", siteModel,this, new SourceCode());
            new Thread(() -> indexSite(indexingURL, id)).start();
        }

        return new GeneralResponse(true);
    }

    private void indexSite(IndexingURL indexingURL, int siteId) {
        indexingURL.fork();
        indexingURL.join();

        if (!isIndexing.get()) {
            changeSiteStatus(siteId, Status.FAILED,"Индексация остановлена пользователем", new Date());
        }
        else {
            changeSiteStatus(siteId, Status.INDEXED,"", new Date());
        }

        if (countIndexingSites.decrementAndGet() == 0) {
            isIndexing.set(false);
        }
    }

    @Override
    public GeneralResponse stopIndexing() {
        if (!isIndexing.get()) {
            return new GeneralResponse(false,"Индексация не запущена");
        }
        isIndexing.set(false);
        return new GeneralResponse(true);
    }

    @Override
    public GeneralResponse indexPage(String url) {
        for (Site site : sitesList.getSites()) {
            if (url.contains(site.getUrl())) {
                SiteModel siteModel = siteRepository.getByName(site.getName());
                String path = url.replace(site.getUrl(), "");
                PageModel pageModel = pageRepository.findByPathAndSiteId(path, siteModel.getId());

                if (pageModel != null) {
                    lemmaRepository.changeLemmasFrequencyByPageId(pageModel.getId());
                    lemmaRepository.deleteAllByFrequency(0);
                    pageRepository.deleteById(pageModel.getId());
                }

                IndexingURL indexingURL = new IndexingURL(path, siteModel, this, new SourceCode());
                indexingURL.indexingPage();
                return new GeneralResponse(true);
            }
        }
        return new GeneralResponse(false,"Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }

    public synchronized boolean savePage(PageModel page, int id) {
        if (isNewPage(page.getPath(), id)) {
            pageRepository.save(page);
            siteRepository.changeStatusTimeById(id, new Date());
            return true;
        }
        return false;
    }

    public synchronized LemmaModel saveLemma(LemmaModel lemma) {
        LemmaModel lemmaModel = lemmaRepository.findByLemmaAndSiteId(lemma.getLemma(), lemma.getSite().getId());
        if (lemmaModel == null) {
            lemma.setFrequency(1);
            lemmaRepository.save(lemma);
            return lemma;
        }
        lemmaRepository.changeFrequencyById(lemmaModel.getId());
        return lemmaModel;
    }

    public void saveIndex(IndexModel index) {
        indexRepository.save(index);
    }

    public boolean isNewPage(String path, int siteId) {
        return pageRepository.findByPathAndSiteId(path, siteId) == null;
    }

    public void changeSiteStatus(int id, Status status, String error, Date date) {
        siteRepository.changeStatusById(id, status);
        if (!error.isEmpty()) {
            siteRepository.changeLastErrorById(id, error);
        }
        siteRepository.changeStatusTimeById(id, date);
    }
}