package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.Indexing.LemmasList;
import searchengine.dto.GeneralResponse;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchPage;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public Object search(String query, String site, int offset, int limit) {
        if (query.isEmpty()) {
            return new GeneralResponse(false,"Задан пустой поисковый запрос");
        }
        Set<String> lemmas = LemmasList.getLemmas(query).keySet();
        List<SearchPage> searchPages = new ArrayList<>();

        for (String lemma : lemmas){
            List<IndexModel> indexModels = new ArrayList<>();
            List<LemmaModel> lemmaModels;
            if (site == null) {
                lemmaModels = lemmaRepository.findAllByLemma(lemma);
            }
            else {
                lemmaModels = lemmaRepository.findAllByLemmaAndSiteId(lemma, siteRepository.getIdByUrl(site));
            }
            for (LemmaModel lemmaModel : lemmaModels) {
                indexModels.addAll(indexRepository.findAllByLemmaId(lemmaModel.getId()));
            }

            if (searchPages.isEmpty()) {
                for (IndexModel indexModel : indexModels) {
                    searchPages.add(new SearchPage(indexModel.getPage(), indexModel.getRank()));
                }
            }
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
                    if (mismatch) {
                        iterator.remove();
                    }
                }
            }
            if (searchPages.isEmpty()) {
                return new GeneralResponse(false, "Страницы не найдены");
            }
        }

        if (limit == 0) {
            limit = 20;
        }
        if (offset >= searchPages.size()) {
            return new GeneralResponse(false, "Значение offset превышено");
        }

        searchPages.sort((p1, p2) -> Float.compare(p2.getRank(), p1.getRank()));
        float maxRank = searchPages.get(0).getRank();
        int num = 1;

        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        searchResponse.setCount(searchPages.size());
        for (SearchPage searchPage : searchPages) {
            if (num > offset + limit) {
                break;
            }
            else {
                if (num > offset) {
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
                num++;
            }
        }
        return searchResponse;
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
        if (text.length() > index + 250) {
            words = text.substring(index, index + 250).split(" ");
        }
        else {
            words = text.substring(index).split(" ");
        }

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
                if (!isLemma) {
                    builder.append(word).append(" ");
                }
            }
            else {
                builder.append("...");
                break;
            }
        }

        return builder.toString();
    }
}