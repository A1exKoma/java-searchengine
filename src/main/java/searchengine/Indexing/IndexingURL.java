package searchengine.Indexing;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.services.SiteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class IndexingURL extends RecursiveAction {
    private final String path;
    private final SiteModel site;
    private final SiteService siteService;
    private Document document;
    private final SourceCode sourceCode;

    public IndexingURL(String path, SiteModel site, SiteService siteService) {
        this.path = path;
        this.site = site;
        this.siteService = siteService;
        sourceCode = new SourceCode();
    }

    @Override
    protected void compute() {
        if (!siteService.isStop()) {
            if (indexingPage() && !siteService.isStop()) {
                Elements elements = document.select("a[href]");
                List<IndexingURL> taskList = new ArrayList<>();
                for (Element element : elements) {
                    String newURL = element.attr("abs:href");
                    String path = newURL.replace(site.getUrl(), "");
                    if (newURL.contains(site.getUrl()) && (newURL.endsWith("/") || newURL.endsWith(".html")) && siteService.isNewPage(path, site.getId())) {
                        IndexingURL task = new IndexingURL(path, site, siteService);
                        taskList.add(task);
                        task.fork();
                    }
                }
                for (IndexingURL task : taskList) task.join();
            }
        }
    }

    public boolean indexingPage() {
        System.out.println(site.getUrl() + path + ": Open");
        Connection.Response response;
        try {
            response = sourceCode.getSourceCode(site.getUrl() + path);
            document = response.parse();
        } catch (InterruptedException | IOException e) {
            siteService.changeSiteStatus(site.getId(), Status.FAILED, "Страница сайта недоступна", new Date());
            siteService.setCount(siteService.getCount() - 1);
            if (siteService.getCount() == 0) siteService.setIndexing(false);
            throw new RuntimeException(e);
        }

        PageModel pageModel = new PageModel();
        pageModel.setPath(path);
        pageModel.setSite(site);
        pageModel.setCode(response.statusCode());
        pageModel.setContent(document.toString());

        if (siteService.savePage(pageModel, site.getId())) {
            HashMap<String, Integer> lemmas = siteService.getLemmas(document.text());

            for (String lemma : lemmas.keySet()) {
                LemmaModel lemmaModel = new LemmaModel();
                lemmaModel.setSite(site);
                lemmaModel.setLemma(lemma);
                lemmaModel = siteService.saveLemma(lemmaModel);

                IndexModel indexModel = new IndexModel();
                indexModel.setLemma(lemmaModel);
                indexModel.setPage(pageModel);
                indexModel.setRank(lemmas.get(lemma));
                siteService.saveIndex(indexModel);
            }
            System.out.println(site.getUrl() + path + ": Close");
            return true;
        }
        System.out.println(site.getUrl() + path + ": Close");
        return false;
    }
}