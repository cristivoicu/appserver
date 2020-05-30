package ro.lic.server.websocket.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
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

    private static Authoriser INSTANCE = new Authoriser();

    private static Algorithm algorithm;

    private Authoriser(){
        algorithm = Algorithm.HMAC256("appsAlgPass");
    }

    public static Authoriser getInstance(){
        return INSTANCE;
    }

    public Algorithm getAlgorithm(){
        return algorithm;
    }

    private static boolean checkToken(String token, Role requiredRole){
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("AppServer")
                .build();
        try {
            DecodedJWT jwt = verifier.verify(token);
            Claim role = jwt.getClaim("role");
            return Role.valueOf(role.asString()) == requiredRole;
        }
        catch (JWTVerificationException e){
            return false;
        }
    }

    private static boolean checkToken(String token){
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("AppServer")
                .build();
        try {
            DecodedJWT jwt = verifier.verify(token);
            return true;
        }
        catch (JWTVerificationException e){
            return false;
        }
    }

    public static boolean checkToken(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;
        return checkToken(token);
    }

    public static boolean authoriseEnroll(UserSession session, String token) {
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authorisePlayVideo(UserSession session, String token) {
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseListRecordedVideos(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseListUsers(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseListTimeline(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseEditUser(UserSession session, String token) {
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseRequestUserData(UserSession session, String requestedUsername, String token){
        if(!session.getToken().equals(token))
            return false;
        if(session.getUsername().equals(requestedUsername) && checkToken(token)){
            return true;
        }
        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseRequestUserToStream(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseRequestLiveStreamers(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseToDisableUser(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseRequestLocation(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseAccessMapItems(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }

    public static boolean authoriseSubscribe(UserSession session, String token){
        if(!session.getToken().equals(token))
            return false;

        return checkToken(token, Role.ADMIN);
    }
}
