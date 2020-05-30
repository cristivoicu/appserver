package ro.lic.server.model.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ro.lic.server.model.dao.ServerLogDao;
import ro.lic.server.model.enums.Importance;
import ro.lic.server.model.enums.ServerLogActionType;
import ro.lic.server.model.tables.ServerLog;
import ro.lic.server.model.tables.User;

import java.util.Date;
import java.util.List;

@Component
public class ServerLogRepository {
    @Autowired
    private ServerLogDao serverLogDao;

    public List<ServerLog> getLogOnDate(String date){
        return serverLogDao.getLogOnDate(date);
    }

    public List<ServerLog> getLogOnDateForUser(String date, User user){
        return serverLogDao.getTimeLineForUserOnDate(user.getId(), date);
    }

    public void userLogin(User user) {
        ServerLog serverLog = new ServerLog(new Date(),
                user,
                String.format("User %s has log in.",
                        user.getUsername()),
                Importance.LOW,
                ServerLogActionType.LOGIN);
        serverLogDao.save(serverLog);
    }

    public void userLogout(User user) {
        ServerLog serverLog = new ServerLog(new Date(),
                        user,
                        String.format("User %s has log out.",
                        user.getUsername()),
                        Importance.LOW,
                        ServerLogActionType.LOGIN);
        serverLogDao.save(serverLog);
    }

    public void userStartStreaming(User user) {
        ServerLog serverLog = new ServerLog(new Date(), user,
                String.format("User %s has started streaming", user.getUsername()),
                Importance.MEDIUM, ServerLogActionType.MEDIA);
        serverLogDao.save(serverLog);
    }

    public void userEndStreaming(User user) {
        ServerLog serverLog = new ServerLog(new Date(), user,
                String.format("User %s has ended streaming", user.getUsername()),
                Importance.MEDIUM, ServerLogActionType.MEDIA);
        serverLogDao.save(serverLog);
    }

    public void userStartedPlayback(User user) {
        ServerLog serverLog = new ServerLog(new Date(), user,
                String.format("User %s has started playback.", user.getUsername()),
                Importance.MEDIUM, ServerLogActionType.MEDIA);
        serverLogDao.save(serverLog);
    }

    public void userEndedPlayback(User user) {
        ServerLog serverLog = new ServerLog(new Date(), user,
                String.format("User %s has ended playback.", user.getUsername()),
                Importance.MEDIUM, ServerLogActionType.MEDIA);
        serverLogDao.save(serverLog);
    }

    public void onUserEnrolled(User user) {
        ServerLog serverLog = new ServerLog(new Date(),
                user,
                String.format("User %s has enrolled new user in application.",
                        user.getUsername()),
                Importance.MEDIUM,
                ServerLogActionType.MEDIA);
        serverLogDao.save(serverLog);
    }

    public void onUserDisabled(User user, String userTarget) {
        ServerLog serverLog = new ServerLog(new Date(),
                user,
                String.format("User %s has disabled user %s from application.",
                        user.getUsername(), userTarget),
                Importance.MEDIUM,
                ServerLogActionType.MEDIA);
        serverLogDao.save(serverLog);
    }

    public void onUserEdited(User user, User userTarget) {
        ServerLog serverLog = new ServerLog(new Date(),
                user,
                String.format("User %s has edited attributes for %s.",
                        user.getUsername(), userTarget.getUsername()),
                Importance.MEDIUM,
                ServerLogActionType.MEDIA);
        serverLogDao.save(serverLog);
    }

    public void unauthorisedAction(User user, String action){
        ServerLog serverLog = new ServerLog(new Date(),
                user,
                String.format("User %s had denied action: %s", user.getUsername(), action),
                Importance.HIGN,
                ServerLogActionType.SECURITY);
        serverLogDao.save(serverLog);
    }
}
