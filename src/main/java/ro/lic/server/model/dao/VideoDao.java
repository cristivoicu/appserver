package ro.lic.server.model.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.lic.server.model.tables.Video;

@Repository
public interface VideoDao extends CrudRepository<Video, Long> {
}
