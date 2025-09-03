package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Integer> {
    List<IndexModel> findAllByLemmaId(int id);
}