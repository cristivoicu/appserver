package ro.lic.server.model.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ro.lic.server.model.tables.Action;

import java.util.List;

public interface ActionDao extends CrudRepository<Action, Long> {

    @Query(value = "select * from actions where user_id = :userId and Date(date) = :date", nativeQuery = true)
    List<Action> getTimeLineForUserOnDate(@Param("userId") long id, @Param("date") String date);
}
