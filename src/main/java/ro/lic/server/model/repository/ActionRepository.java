package ro.lic.server.model.repository;

import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ro.lic.server.model.dao.ActionDao;
import ro.lic.server.model.tables.Action;
import ro.lic.server.model.tables.User;
import ro.lic.server.model.tables.Video;

import java.util.Date;
import java.util.List;

/***/
@Component
public class ActionRepository {
    @Autowired
    private ActionDao actionDao;

    public void userLogin(User user){
        Action action = new Action("User logged in.", new Date(), user);
        actionDao.save(action);
    }

    public void userLogout(User user){
        Action action = new Action("User logged out.", new Date(), user);
        actionDao.save(action);
    }

    public void userStartedRecordingSession(User user){
        Action action = new Action("User started recording session.", new Date(), user);
        actionDao.save(action);
    }

    public void userEndedRecordingSession(User user){
        Action action = new Action("User ended recording session.", new Date(), user);
        actionDao.save(action);
    }

    public void onEnrolledUser(User user){
        Action action = new Action("User enrolled a new user in application.", new Date(), user);
        actionDao.save(action);
    }

    public void onDisabledUser(User user, String username){
        Action action = new Action(String.format("Disabled %s acount", username), new Date(), user);
        actionDao.save(action);
    }

    public void userStartedPlaybackVideo(User user){
        Action action = new Action("User started playback video.", new Date(), user);
        actionDao.save(action);
    }

    public void userEndedPlaybackVideo(User user){
        Action action = new Action("User ended playback video.", new Date(), user);
        actionDao.save(action);
    }

    public void onEditUser(User doneBy, User targetUser){
        Action action = new Action("Edited attributes of " + targetUser.getUsername(), new Date(), doneBy);
        actionDao.save(action);
    }

    public List<Action> getTimeLineForUserOnDate(User user, String date){
        System.out.println(String.format("USER: %s, DATE: %s", user, date));
        return actionDao.getTimeLineForUserOnDate(user.getId(), date);
    }
}
