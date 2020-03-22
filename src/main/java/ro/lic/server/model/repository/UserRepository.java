package ro.lic.server.model.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import ro.lic.server.model.Roles;
import ro.lic.server.model.dao.UserDao;
import ro.lic.server.model.tables.User;

import java.util.List;

@Component
public class UserRepository {
    @Autowired
    UserDao userDao;

    public void addUser(User user){
        userDao.save(user);
    }

    public void updateRole(User user, Roles newRole) {
        userDao.updateRole(user.getUsername(), newRole);
    }

    public void updateRole(String userName, Roles newRole) {
        userDao.updateRole(userName, newRole);
    }

    public Roles authenticate(String username, String password){
        User user = userDao.getUserByUsername(username);
        String hashPassword = user.getPassword();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, hashPassword) ? user.getRole() : null;
    }

    public Roles getUserRoleByUsername(String username){
        return userDao.getUserRoleByUsername(username);
    }

    public List<User> getAllUsers(){
        return userDao.findAll();
    }

    public User getUser(String username){
        return userDao.getUserByUsername(username);
    }
}
