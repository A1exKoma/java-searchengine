package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaModel;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    LemmaModel findByLemmaAndSiteId(String lemma, int siteId);

    void deleteAllByFrequency(int frequency);

    @Modifying
    @Transactional
    @Query(nativeQuery = true,
            value = "UPDATE lemmas SET lemmas.frequency = lemmas.frequency + 1 WHERE lemmas.id = :id")
    void changeFrequencyById (@Param("id") int id);

    @Modifying
    @Transactional
    @Query(nativeQuery = true,
            value = "UPDATE lemmas AS lem JOIN indexes_list AS ind ON lem.id = ind.lemma_id " +
                    "SET lem.frequency = lem.frequency - 1 WHERE ind.page_id = :id")
    void changeLemmasFrequencyByPageId (@Param("id") int id);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM lemmas WHERE site_id = :id")
    int countBySiteId (@Param("id") int id);

    List<LemmaModel> findAllByLemma(String lemma);

    List<LemmaModel> findAllByLemmaAndSiteId(String lemma, int siteId);
}