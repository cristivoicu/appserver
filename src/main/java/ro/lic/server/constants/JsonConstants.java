package ro.lic.server.constants;

public class JsonConstants {
    // events
    public static final String EVENT_CALL = "call";
    public static final String EVENT_ICE_CANDIDATE = "onIceCandidate";
    public static final String EVENT_INCOMING_CALL_RESPONSE = "incomingCallResponse";
    public static final String EVENT_STOP = "stop";
    public static final String EVENT_REGISTER = "register";
    public static final String EVENT_LIST_USERS = "listUsers";
    /**
     * Event used to register new user in server database
     */
    public static final String EVENT_ENROLL = "enroll";

    public static final String EVENT_START_REC = "startRec";
    public static final String EVENT_STOP_REC = "stopRec";
    public static final String EVENT_PLAY = "play";
    public static final String EVENT_STOP_PLAY = "stopPlay";

    // messaged ids that server sends to its clients
    public static final String ENROLL_SUCCESS = "enroll_success";
}
