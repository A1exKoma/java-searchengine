package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;

@Repository
public interface PageRepository extends JpaRepository<PageModel, Integer> {
    PageModel findByPathAndSiteId(String path, int siteId);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) FROM pages WHERE site_id = :id")
    int countBySiteId (@Param("id") int id);
}