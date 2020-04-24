package ro.lic.server.model.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ro.lic.server.model.enums.Role;
import ro.lic.server.model.enums.Status;
import ro.lic.server.model.dao.UserDao;
import ro.lic.server.model.tables.User;

import javax.annotation.Nonnull;
import java.util.List;

@Component
public class UserRepository {
    @Autowired
    UserDao userDao;

    public void addUser(User user){
        userDao.save(user);
    }

    public void updateRole(User user, Role newRole) {
        userDao.updateRole(user.getUsername(), newRole.name());
    }

    public void updateRole(String userName, Role newRole) {
        userDao.updateRole(userName, newRole.name());
    }

    public Role authenticate(String username, String password){
        User user = userDao.getUserByUsername(username);
        if(user == null)
            return null;
        String hashPassword = user.getPassword();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, hashPassword) ? Role.valueOf(user.getRole()) : null;
    }

    public Role getUserRoleByUsername(String username){
        return userDao.getUserRoleByUsername(username);
    }

    public List<User> getAllUsers(){
        return userDao.findAll();
    }

    public List<User> getAllUsersByRole(Role role){
        return userDao.getAllUsersByRole(role);
    }

    public User getUser(String username){
        return userDao.getUserByUsername(username);
    }

    public int updateUser(@Nonnull User user){
        return userDao.updateUser(user.getUsername(),
                user.getName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getProgramStart(),
                user.getProgramEnd(),
                user.getRole());
    }

    public void setUserOnline(String username){
        userDao.updateOnlineStatus(username, Status.ONLINE.name());
    }

    public void setUserOffline(String username){
        userDao.updateOnlineStatus(username, Status.OFFLINE.name());
    }

    public void disableUser(String username){
        userDao.updateOnlineStatus(username, Status.DISABLED.name());
    }
}
