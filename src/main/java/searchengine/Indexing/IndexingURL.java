package searchengine.Indexing;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.*;
import searchengine.services.IndexServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class IndexingURL extends RecursiveAction {
    private final String path;
    private final SiteModel site;
    private final IndexServiceImpl indexServiceImpl;
    private Document document;
    private final SourceCode sourceCode;
    private final Logger logger = LoggerFactory.getLogger(IndexingURL.class);

    public IndexingURL(String path, SiteModel site, IndexServiceImpl indexServiceImpl, SourceCode sourceCode) {
        this.path = path;
        this.site = site;
        this.indexServiceImpl = indexServiceImpl;
        this.sourceCode = sourceCode;
    }

    @Override
    protected void compute() {
        if (indexServiceImpl.isIndexing.get()) {
            if (indexingPage() && indexServiceImpl.isIndexing.get()) {
                Elements elements = document.select("a[href]");
                List<IndexingURL> taskList = new ArrayList<>();
                for (Element element : elements) {
                    String newURL = element.attr("abs:href");
                    String path = newURL.replace(site.getUrl(), "");
                    if (newURL.contains(site.getUrl()) && (newURL.endsWith("/") || newURL.endsWith(".html")) && indexServiceImpl.isNewPage(path, site.getId())) {
                        IndexingURL task = new IndexingURL(path, site, indexServiceImpl, sourceCode);
                        taskList.add(task);
                        task.fork();
                    }
                }
                for (IndexingURL task : taskList) {
                    task.join();
                }
            }
        }
    }

    public boolean indexingPage() {
        Connection.Response response;
        try {
            response = sourceCode.getSourceCode(site.getUrl() + path);
            document = response.parse();
        } catch (InterruptedException | IOException e) {
            logger.error("{}{}", site.getUrl(), path);
            indexServiceImpl.changeSiteStatus(site.getId(), Status.FAILED, "Страница сайта недоступна", new Date());
            if (indexServiceImpl.countIndexingSites.decrementAndGet() == 0) {
                indexServiceImpl.isIndexing.set(false);
            }
            throw new RuntimeException(e);
        }

        PageModel pageModel = new PageModel();
        pageModel.setPath(path);
        pageModel.setSite(site);
        pageModel.setCode(response.statusCode());
        pageModel.setContent(document.toString());

        if (indexServiceImpl.savePage(pageModel, site.getId())) {
            HashMap<String, Integer> lemmas = LemmasList.getLemmas(document.text());

            for (String lemma : lemmas.keySet()) {
                LemmaModel lemmaModel = new LemmaModel();
                lemmaModel.setSite(site);
                lemmaModel.setLemma(lemma);
                lemmaModel = indexServiceImpl.saveLemma(lemmaModel);

                IndexModel indexModel = new IndexModel();
                indexModel.setLemma(lemmaModel);
                indexModel.setPage(pageModel);
                indexModel.setRank(lemmas.get(lemma));
                indexServiceImpl.saveIndex(indexModel);
            }
            return true;
        }
        return false;
    }
}