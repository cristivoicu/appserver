package ro.lic.server.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.lic.server.model.tables.Video;

import java.util.List;

@Repository
public interface VideoDao extends CrudRepository<Video, Long> {

    @Query(value = "select * from videos where user_id = :userId", nativeQuery = true)
    List<Video> getVideoForUser(Long userId);

    @Query(value = "select * from videos where user_id = :userId and Date(date) = :date", nativeQuery = true)
    List<Video> getVideoForUserAtDate(Long userId, String date);

    @Query(value = "SELECT * from videos", nativeQuery = true)
    List<Video> getAllVideos();

    @Query(value = "select * from videos where Date(date) = :date", nativeQuery = true)
    List<Video> getAllVideosAtDate(String date);

}
