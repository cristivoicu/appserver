package ro.lic.server.model.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ro.lic.server.model.tables.Action;
import ro.lic.server.model.tables.ServerLog;

import java.util.List;

public interface ServerLogDao extends CrudRepository<ServerLog, Long> {

    @Query(value = "select * from server_log where Date(datetime) = :date order by Time(datetime) DESC",
            nativeQuery = true)
    List<ServerLog> getLogOnDate(@Param("date") String date);

    @Query(value = "select * from server_log where user_id = :userId and Date(datetime) = :date order by Time(datetime) DESC",
            nativeQuery = true)
    List<ServerLog> getTimeLineForUserOnDate(@Param("userId") long id, @Param("date") String date);
}
