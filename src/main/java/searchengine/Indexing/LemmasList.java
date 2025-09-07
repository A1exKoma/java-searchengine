package searchengine.Indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class LemmasList {
    private static final Logger logger = LoggerFactory.getLogger(LemmasList.class);
    public static HashMap<String, Integer> getLemmas(String text) {
        String[] words = text.replaceAll("[\\p{P}\\p{N}+]", " ").toLowerCase().split(" ");
        logger.info("{}", words.length);
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
                    for (String particle : particlesNamesRu) {
                        if (luceneMorphRu.getMorphInfo(word).toString().contains(particle)) {
                            isParticle = true;
                            break;
                        }
                    }
                    if (!isParticle) {
                        List<String> wordBaseForms = luceneMorphRu.getNormalForms(word);
                        for (String lemma : wordBaseForms) {
                            if (lemmas.containsKey(lemma)) {
                                lemmas.put(lemma, lemmas.get(lemma) + 1);
                            }
                            else {
                                lemmas.put(lemma, 1);
                            }
                        }
                    }
                }
                else {
                    if (word.matches("[a-z]*")) {
                        boolean isParticle = false;
                        for (String particle : particlesNamesEng) {
                            if (luceneMorphEng.getMorphInfo(word).toString().contains(particle)) {
                                isParticle = true;
                                break;
                            }
                        }
                        if (!isParticle) {
                            List<String> wordBaseForms = luceneMorphEng.getNormalForms(word);
                            for (String lemma : wordBaseForms) {
                                if (lemmas.containsKey(lemma)) {
                                    lemmas.put(lemma, lemmas.get(lemma) + 1);
                                }
                                else {
                                    lemmas.put(lemma, 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        return lemmas;
    }
}