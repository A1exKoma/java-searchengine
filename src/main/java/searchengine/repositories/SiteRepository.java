package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteModel;
import searchengine.model.Status;

import java.util.Date;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {
    SiteModel getByName(String name);

    @Modifying
    @Transactional
    @Query(nativeQuery = true,
            value = "UPDATE sites SET sites.status = :#{#status?.name()} WHERE sites.id = :id")
    void changeStatusById(@Param("id") int id, @Param("status") Status status);

    @Modifying
    @Transactional
    @Query(nativeQuery = true,
            value = "UPDATE sites SET sites.status_time = :time WHERE sites.id = :id")
    void changeStatusTimeById(@Param("id") int id, @Param("time") Date date);

    @Modifying
    @Transactional
    @Query(nativeQuery = true,
            value = "UPDATE sites SET sites.last_error = :error WHERE sites.id = :id")
    void changeLastErrorById(@Param("id") int id, @Param("error") String error);

    @Query(nativeQuery = true,
            value = "SELECT id FROM sites WHERE sites.url = :url")
    int getIdByUrl(@Param("url") String url);
}