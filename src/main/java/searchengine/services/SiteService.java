package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.Indexing.IndexingURL;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.GeneralResponse;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchPage;
import searchengine.dto.search.SearchResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SiteService {
    @Getter
    private final PageRepository pageRepository;
    @Getter
    private final SiteRepository siteRepository;
    @Getter
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    @Getter
    @Setter
    private boolean isIndexing = false;
    @Getter
    private boolean isStop = false;
    @Getter
    @Setter
    private int count = 0;

    public void startIndexing() {
        isIndexing = true;

        for (Site site : sitesList.getSites()) {
            count++;

            SiteModel siteModel = siteRepository.getByName(site.getName());
            if (siteModel != null) siteRepository.deleteById(siteModel.getId());

            siteModel = new SiteModel();
            siteModel.setName(site.getName());
            siteModel.setUrl(site.getUrl());
            siteModel.setStatus(Status.INDEXING);
            siteModel.setStatusTime(new Date());
            siteRepository.save(siteModel);
            int id = siteModel.getId();

            IndexingURL indexingURL = new IndexingURL("/", siteModel,this);
            new Thread(() -> indexSite(indexingURL, id)).start();
        }
    }

    private void indexSite(IndexingURL indexingURL, int siteId) {
        indexingURL.fork();
        indexingURL.join();

        if (isStop) changeSiteStatus(siteId, Status.FAILED,"Индексация остановлена пользователем", new Date());
        else changeSiteStatus(siteId, Status.INDEXED,"", new Date());

        if (--count == 0) isIndexing = false;
    }

    public void stopIndexing() {
        isStop = true;
    }

    public boolean indexPage(String url) {
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

                IndexingURL indexingURL = new IndexingURL(path, siteModel, this);
                indexingURL.indexingPage();
                return true;
            }
        }
        return false;
    }

    public Object search(String query, String site) {
        if (query.isEmpty()) return new GeneralResponse(false,"Задан пустой поисковый запрос");
        Set<String> lemmas = getLemmas(query).keySet();
        List<SearchPage> searchPages = new ArrayList<>();

        for (String lemma : lemmas){
            List<IndexModel> indexModels = new ArrayList<>();
            List<LemmaModel> lemmaModels;
            if (site == null) lemmaModels = lemmaRepository.findAllByLemma(lemma);
            else lemmaModels = lemmaRepository.findAllByLemmaAndSiteId(lemma, siteRepository.getIdByUrl(site));
            for (LemmaModel lemmaModel : lemmaModels) indexModels.addAll(indexRepository.findAllByLemmaId(lemmaModel.getId()));

            if (searchPages.isEmpty()) for (IndexModel indexModel : indexModels) searchPages.add(new SearchPage(indexModel.getPage(), indexModel.getRank()));
            else {
                Iterator<SearchPage> iterator = searchPages.iterator();
                while (iterator.hasNext()) {
                    SearchPage searchPage = iterator.next();
                    boolean mismatch = true;
                    for (IndexModel indexModel : indexModels) {
                        if (searchPage.getPageModel().getId() == indexModel.getPage().getId()) {
                            mismatch = false;
                            searchPage.setRank(searchPage.getRank() + indexModel.getRank());
                            indexModels.remove(indexModel);
                            break;
                        }
                    }
                    if (mismatch) iterator.remove();
                }
            }
            if (searchPages.isEmpty()) return new GeneralResponse(false, "Страницы не найдены");
        }

        searchPages.sort((p1, p2) -> Float.compare(p2.getRank(), p1.getRank()));
        float maxRank = searchPages.get(0).getRank();

        SearchResponse searchResponse = new SearchResponse();
        for (SearchPage searchPage : searchPages) {
            SearchData searchData = new SearchData();
            searchData.setSite(searchPage.getPageModel().getSite().getUrl());
            searchData.setSiteName(searchPage.getPageModel().getSite().getName());
            searchData.setUrl(searchPage.getPageModel().getPath());
            Document document = Jsoup.parse(searchPage.getPageModel().getContent());
            searchData.setTitle(document.title());
            searchData.setSnippet(getSnippet(document.text(), lemmas));
            searchData.setRelevance(searchPage.getRank() / maxRank);
            searchResponse.addData(searchData);
        }
        searchResponse.setCount(searchResponse.getData().size());
        searchResponse.setResult(true);
        return searchResponse;
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
        if (!error.isEmpty()) siteRepository.changeLastErrorById(id, error);
        siteRepository.changeStatusTimeById(id, date);
    }

    public HashMap<String, Integer> getLemmas(String text) {
        String[] words = text.replaceAll("[\\p{P}\\p{N}+]", " ").toLowerCase().split(" ");
        String[] particlesNamesRu = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ", "МС"};
        String[] particlesNamesEng = new String[]{"ARTICLE", "CONJ", "ADVERB", "VBE", "PART"};
        LuceneMorphology luceneMorphRu;
        LuceneMorphology luceneMorphEng;
        {
            try {
                luceneMorphRu = new RussianLuceneMorphology();
                luceneMorphEng = new EnglishLuceneMorphology();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (!word.isBlank()) {
                if (word.matches("[а-я]*")) {
                    boolean isParticle = false;
                    for (String particle : particlesNamesRu) if (luceneMorphRu.getMorphInfo(word).toString().contains(particle)) {
                        isParticle = true;
                        break;
                    }
                    if (!isParticle) {
                        List<String> wordBaseForms = luceneMorphRu.getNormalForms(word);
                        for (String lemma : wordBaseForms) {
                            if (lemmas.containsKey(lemma)) lemmas.put(lemma, lemmas.get(lemma) + 1);
                            else lemmas.put(lemma, 1);
                        }
                    }
                }
                else if (word.matches("[a-z]*")) {
                    boolean isParticle = false;
                    for (String particle : particlesNamesEng) if (luceneMorphEng.getMorphInfo(word).toString().contains(particle)) {
                        isParticle = true;
                        break;
                    }
                    if (!isParticle) {
                        List<String> wordBaseForms = luceneMorphEng.getNormalForms(word);
                        for (String lemma : wordBaseForms) {
                            if (lemmas.containsKey(lemma)) lemmas.put(lemma, lemmas.get(lemma) + 1);
                            else lemmas.put(lemma, 1);
                        }
                    }
                }
            }
        }
        return lemmas;
    }

    public String getSnippet(String text, Set<String> lemmas) {
        String lowText = text.replaceAll("[\\p{P}\\p{N}+]", " ").toLowerCase();
        List<Integer> indexes = new ArrayList<>();

        for (String lemma : lemmas) {
            int lastIndex = 0;
            while(lastIndex != -1) {
                lastIndex = lowText.indexOf(lemma,lastIndex);
                if (lastIndex != -1) {
                    indexes.add(lastIndex);
                    lastIndex += 1;
                }
            }
        }
        indexes.sort(Comparator.comparingInt(i -> i));

        int index = indexes.get(0);
        int maxMatches = 1;
        for (int i = 0; i < indexes.size() - maxMatches; i++) {
            int length = 0;
            int count = 1;
            int j = i;
            while (length < 120 && j < indexes.size() - 1) {
                length = length + indexes.get(j+1) - indexes.get(j);
                if (length < 120) {
                    count ++;
                    if (maxMatches < count) {
                        maxMatches = count;
                        index = indexes.get(i);
                    }
                    j++;
                }
            }
        }

        String[] words;
        if (text.length() > index + 250) words = text.substring(index, index + 250).split(" ");
        else words = text.substring(index).split(" ");

        StringBuilder builder = new StringBuilder("... ");
        for (String word : words) {
            if (builder.length() + word.length() < 200){
                boolean isLemma = false;
                for (String lemma : lemmas) {
                    if (word.toLowerCase().contains(lemma)) {
                        index = word.toLowerCase().indexOf(lemma);
                        builder.append(word, 0, index).append("<b>").append(word, index, index + lemma.length())
                                .append("</b>").append(word, index + lemma.length(), word.length()).append(" ");
                        isLemma = true;
                        break;
                    }
                }
                if (!isLemma) builder.append(word).append(" ");
            }
            else {
                builder.append("...");
                break;
            }
        }

        return builder.toString();
    }
}