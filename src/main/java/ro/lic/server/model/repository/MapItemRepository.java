package ro.lic.server.model.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ro.lic.server.model.dao.MapItemDao;
import ro.lic.server.model.tables.Coordinates;
import ro.lic.server.model.tables.MapItem;

import java.util.List;

@Component
public class MapItemRepository {

    @Autowired
    private MapItemDao mapItemDao;

    public void addMapItem(MapItem mapItem){
        mapItemDao.save(mapItem);
    }

    public List<MapItem> getMapItems(){
        return mapItemDao.getAll();
    }

    public List<Coordinates> getCoordinates(Long id){
        return mapItemDao.getCoordinates(id);
    }

    public void clearTable(){
        mapItemDao.deleteAll();
    }
}
