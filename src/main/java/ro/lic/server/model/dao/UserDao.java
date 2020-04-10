package ro.lic.server.model.dao;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.lic.server.model.Roles;
import ro.lic.server.model.tables.User;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface UserDao extends CrudRepository<User, Long> {
    @Query(value = "select * from users", nativeQuery = true)
    List<User> findAll();

    @Modifying
    @Transactional
    @Query(value = "UPDATE USERS u SET u.role = :role where u.username = :username", nativeQuery = true)
    int updateRole(@Param("username") String username, @Param("role") Roles role);

    @Modifying
    @Transactional
    @Query(value = "UPDATE USERS u SET u.isOnline = :newStatus where u.username = :username", nativeQuery = true)
    int updateStatus(@Param("username") String username, @Param("newStatus") boolean newStatus);

    @Modifying
    @Transactional
    @Query(value = "UPDATE USERS u SET " +
            "u.name = :name, " +
            "u.phone_number = :phoneNumber, " +
            "u.address = :address, " +
            "u.program_start = :programStart, " +
            "u.program_end = :programEnd, " +
            "u.role = :role " +
            " WHERE u.username = :username ",
            nativeQuery = true)
    int updateUser(@Param("username") String username,
                   @Param("name") String name,
                   @Param("phoneNumber") String phoneNumber,
                   @Param("address") String address,
                   @Param("programStart") String programStart,
                   @Param("programEnd") String programEnd,
                   @Param("role") Roles role);

    @Query(value = "select role from users where username = :username", nativeQuery = true)
    Roles getUserRoleByUsername(@Param("username") String username);

    @Query(value = "select password from users where username = :username", nativeQuery = true)
    String getUserPasswordByUsername(@Param("username") String username);

    @Query(value = "select * from users where username = :username", nativeQuery = true)
    User getUserByUsername(@Param("username") String username);

    @Query(value = "select * from users where role = :role", nativeQuery = true)
    List<User> getAllUsersByRole(@Param("role") Roles role);

}
