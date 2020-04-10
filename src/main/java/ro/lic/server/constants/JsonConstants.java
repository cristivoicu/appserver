package ro.lic.server.constants;

/**
 * This class encapsulate the static key names for json communication.
 * It contain the following message types:
 * <ul>
 *     <li> <i> events </i> </li>
 *     <li> <i> requests </i> </li>
 * </ul>
 */
public class JsonConstants {
    // events
    public static final String EVENT_ICE_CANDIDATE = "onIceCandidate";
    public static final String EVENT_STOP = "stop";

    public static final String EVENT_UPDATE_USER = "updateUser";

    public static final String EVENT_LIVE_VIDEO = "liveVideo";
    /**
     * Event used to register new user in server database
     */
    public static final String EVENT_ENROLL = "enroll";

    public static final String EVENT_START_REC = "startRec";
    public static final String EVENT_STOP_REC = "stopRec";
    public static final String EVENT_PLAY = "play";
    public static final String EVENT_STOP_PLAY = "stopPlay";

    // events for playback (media player)
    public static final String EVENT_PLAY_VIDEO = "playVideo";
    public static final String EVENT_STOP_VIDEO = "stopVideo";
    public static final String EVENT_PAUSE_VIDEO = "pauseVideo";
    public static final String EVENT_DO_SEEK_VIDEO = "seekVideo";
    public static final String EVENT_RESUME_VIDEO = "resumeVideo";
    public static final String EVENT_GET_POSITION_VIDEO = "getPositionVideo";

    // messaged ids that server sends to its clients
    public static final String ENROLL_SUCCESS = "enroll_success";

    // receiving
    public static final String ICE_FOR_REC = "iceForRec";
    public static final String ICE_FOR_LIVE = "iceForLive";
    public static final String ICE_FOR_PLAY = "iceForPlay";
    //sending
    public static final String SEND_ICE_FOR_REC = "recording";
    public static final String SEND_ICE_FOR_LIVE = "live";
    public static final String SEND_ICE_FOR_PLAY = "play";

    // requests
    public static final String REQ_TIMELINE = "requestTimeline";
    public static final String REQ_LIST_USERS = "listUsers";
    public static final String REQ_LIST_RECORDED_VIDEOS = "recordedVideos";
}
