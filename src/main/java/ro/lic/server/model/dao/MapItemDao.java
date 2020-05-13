package ro.lic.server.model.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.lic.server.model.tables.Coordinates;
import ro.lic.server.model.tables.MapItem;

import java.util.List;

@Repository
public interface MapItemDao extends CrudRepository<MapItem, Long> {

    @Query(nativeQuery = true,
            value = "SELECT * from map_items")
    List<MapItem> getAll();

    @Query(nativeQuery = true,
    value = "select latitude, longitude from map_items as mi inner join coordinates on coordinate_id = :id;")
    List<Coordinates> getCoordinates(Long id);
}
