package ro.lic.server.model;

/**
 * This enum represents the roles that a user can have
 * Every user can have one role, each role defines the authorisation for the user
 * <p> <b>ADMIN:</b> is the user that can:
 * <ul>
 *     <li>create accounts</li>
 *     <li>changing roles for all other users</li>
 *     <li>can see the recordings and live streaming of other users</li>
 *     <li>can see the location of other users</li>
 *     <li>can se the timeline of other users</li>
 *     <li>can turn on the video camera</li>
 * </ul></p>
 *
 * <p> <b>PRIVILEGED_I</b>: It commands the user with a lesser role
 * <ul>
 *     <li> can see location</li>
 *     <li> can see timeline</li>
 *     <li> can see live streaming video</li>
 *     <li> can turn on the video camera</li>
 *     <li> can stream live video</li>
 *     <li> has a time line</li>
 *     <li> sends real time location</li>
 * </ul></p>
 * <p><b>PRIVILEGED_II</b>: Like PRIVILEGED_I, but only to USER role</p>
 * <p><b>USER</b>: Is executor, don't have any rights
 *  <ul>
 *     <li> can stream live video</li>
 *     <li> has a time line</li>
 *     <li> sends real time location</li>
 *  </ul></p>
 *
 * @author Cristian VOICU
 */
public enum Roles {
    ADMIN,
    PRIVILEGED_I,
    PRIVILEGED_II,
    USER
}
