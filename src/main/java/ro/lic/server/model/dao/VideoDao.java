package ro.lic.server.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ro.lic.server.model.tables.Video;

import java.util.List;

@Repository
public interface VideoDao extends JpaRepository<Video, Long> {

    @Query(value = "select * from videos where user_id = :userId", nativeQuery = true)
    List<Video> getVideoForUser(Long userId);

}
