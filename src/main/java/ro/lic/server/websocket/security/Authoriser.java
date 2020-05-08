package ro.lic.server.websocket.security;

import ro.lic.server.model.enums.Role;
import ro.lic.server.websocket.utils.UserSession;

/**
 * Class used to authorise user to do certain tasks
 *
 * The tasks are:
 * <ul>
 *     todo: fill this
 * </ul>
 *
 * @author Cristian VOICU
 */
public class Authoriser {
    public static boolean authoriseEnroll(UserSession session) {
        return session.getRole() == Role.ADMIN;
    }

    public static boolean authorisePlayVideo(UserSession session) {
        return session.getRole() == Role.ADMIN;
    }

    public static boolean authoriseListRecordedVideos(UserSession session){
        return session.getRole() == Role.ADMIN;
    }

    public static boolean authoriseListUsers(UserSession session){
        return session.getRole() == Role.ADMIN;
    }

    public static boolean authoriseListTimeline(UserSession session){
        return session.getRole() == Role.ADMIN;
    }

    public static boolean authoriseEditUser(UserSession session) {
        return session.getRole() == Role.ADMIN;
    }

    public static boolean authoriseRequestUserData(UserSession session, String requestedUsername){
        if(session.getUsername().equals(requestedUsername)){
            return true;
        }
        if(session.getRole() == Role.ADMIN){
            return true;
        }
        return false;
    }

    public static boolean authoriseRequestUserToStream(UserSession session){
        return session.getRole() == Role.ADMIN;
    }

    public static boolean authoriseRequestLiveStreamers(UserSession session){
        return  session.getRole() == Role.ADMIN;
    }

    public static boolean authoriseToDisableUser(UserSession session){
        return session.getRole() == Role.ADMIN;
    }

    public static boolean authoriseRequestLocation(UserSession session){
        return session.getRole() == Role.ADMIN;
    }
}
