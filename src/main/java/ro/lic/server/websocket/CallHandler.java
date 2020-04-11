package ro.lic.server.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.kurento.client.*;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ro.lic.server.model.Role;
import ro.lic.server.model.repository.ActionRepository;
import ro.lic.server.model.repository.UserRepository;
import ro.lic.server.model.repository.VideoRepository;
import ro.lic.server.model.tables.Action;
import ro.lic.server.model.tables.User;
import ro.lic.server.model.tables.Video;
import ro.lic.server.websocket.security.Authoriser;
import ro.lic.server.websocket.utils.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static ro.lic.server.constants.JsonConstants.*;

public class CallHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CallHandler.class);
    private static final Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, h:mm:ss a").setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    private final ConcurrentHashMap<String, RecordMediaPipeline> recordPipeline = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PlayMediaPipeline> playPipelines = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired(required = true)
    private KurentoClient kurento;

    @Autowired(required = true)
    private UserRegistry registry = new UserRegistry();

    @Override
    public void afterConnectionClosed(final WebSocketSession session, CloseStatus status) throws Exception {
        String name = registry.getBySession(session).getUsername();

        User user = userRepository.getUser(name);
        actionRepository.userLogout(user);
        userRepository.setUserOffline(user.getUsername());
        stop(session);
        registry.removeBySession(session);
        System.out.println(String.format("User %s disconnected!", name));
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        String username = session.getPrincipal().getName();
        username = username.substring(username.indexOf("[") + 1, username.indexOf("]"));

        User user = userRepository.getUser(username);
        actionRepository.userLogin(user);

        // check if user is already connected
        if (registry.exists(username)) {
            session.close(CloseStatus.SERVER_ERROR);
            // todo: set log error
        } else {
            System.out.println(String.format("User %s connected!", username));
            UserSession userSession = new UserSession(session, username, userRepository.getUserRoleByUsername(username));
            registry.register(userSession);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonObject receivedMessage = gson.fromJson(message.getPayload(), JsonObject.class);
            UserSession user = registry.getBySession(session);

            if (user != null) {
                System.out.println(String.format("Incoming message from user '%s': %s", user.getUsername(), receivedMessage));
            } else {
                System.out.println(String.format("Incoming message from unknown user: %s}", receivedMessage));
                return;
            }

            switch (receivedMessage.get("id").getAsString()) {
                case EVENT_PLAY_VIDEO:
                    playVideo(user, receivedMessage);
                    break;
                case EVENT_PAUSE_VIDEO:
                    handlePauseVideoMessage(user);
                    break;
                case EVENT_RESUME_VIDEO:
                    handleResumeVideoMessage(user);
                    break;
                case EVENT_STOP_VIDEO:
                    handleStopVideoMessage(user);
                    break;
                case EVENT_DO_SEEK_VIDEO:
                    handleSeekVideoMessage(user, receivedMessage);
                    break;
                case EVENT_GET_POSITION_VIDEO:
                    handleGetVideoPositionMessage(user);
                    break;
                case EVENT_START_REC:
                    System.out.println("Event start rec!");
                    startRec(user, receivedMessage);
                    break;
                case EVENT_STOP_REC:
                    System.out.println("Event stop rec!");
                    stopRec(user, receivedMessage);
                    break;
                case EVENT_LIVE_VIDEO:
                    String from = receivedMessage.get("from").getAsString();
                    String sdpOffer = receivedMessage.get("sdpOffer").getAsString();
                    startLiveVideo(from, sdpOffer, user);
                    break;
                case EVENT_ICE_CANDIDATE: {
                    System.out.println("on Ice" + receivedMessage.toString());
                    String iceFor = receivedMessage.get("for").getAsString();
                    JsonObject candidate = receivedMessage.get("candidate").getAsJsonObject();

                    IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                            candidate.get("sdpMid").getAsString(),
                            candidate.get("sdpMLineIndex").getAsInt());
                    switch (iceFor) {
                        case ICE_FOR_LIVE:
                            user.addCandidateLive(cand);
                            break;
                        case ICE_FOR_PLAY:
                            PlayMediaPipeline playMediaPipeline = playPipelines.get(user.getSessionId());
                            playMediaPipeline.addIceCandidate(cand);
                            break;
                        case ICE_FOR_REC:
                            user.addCandidateRec(cand);
                            break;
                    }
                    break;
                }
                case EVENT_STOP:
                    System.out.println("on stop");
//                    stop(session);
                    break;
                case EVENT_UPDATE_USER:
                    updateUser(user, receivedMessage);
                    break;
                case EVENT_ENROLL:
                    if (Authoriser.authoriseEnroll(user)) {
                        System.out.println("Enroll event: " + receivedMessage.toString());
                        enroll(session, receivedMessage);
                    } else {
                        //todo: log security and disconnect user
                    }
                    break;
                case REQ_LIST_USERS:
                    handleListUsersRequest(user, receivedMessage);
                    break;
                case REQ_LIST_RECORDED_VIDEOS:
                    handleRecordedVideoListRequest(user, receivedMessage);
                    break;
                case REQ_TIMELINE:
                    handleTimelineRequest(user, receivedMessage);
                    break;
                default:
                    System.out.println("Unknown id!");
                    break;
            }
        } catch (JsonSyntaxException e) {
            session.sendMessage(new TextMessage("json error"));
            System.out.println("!!! JSON FROM CLIENT ERROR: " + e.getMessage());
        }
    }

    //region Media player handlers
    /**
     * This method handle a play video request
     * <p> The application server sends a response which must contain:
     * <ul>
     *     <li> id: playResponse</li>
     *     <li> response: accepted (or rejected)</li>
     *     <li> session description offer (only if response is accepted)</li>
     * </ul></p>
     * <p> Also, the application server sends an ice candidate response, which must contain:
     * {@link IceCandidate}
     * <ul>
     *     <li> id: iceCandidate</li>
     *     <li> for: play</li>
     *     <li> the candidate</li>
     * </ul></p>
     * <p> When the peers are connected, in this case the user and Kurento Media Server, the application server
     * send the video parameters {@link VideoInfo}, the message contains:
     * <ul>
     *     <li> id: videoInfo </li>
     *     <li> isSeekable </li>
     *     <li> begin seekable </li>
     *     <li> end seekable </li>
     *     <li> duration </li>
     * </ul></p>
     *
     * @param session         is the user who has send the request
     * @param receivedMessage is the received message
     *                        <p>
     *                        should contain:
     *                        <ul>
     *                        <li> session description offer</li>
     *                        <li> video path: the path where video is save in media server</li>
     *
     *                        </ul></p>
     */
    private void playVideo(UserSession session, JsonObject receivedMessage) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("id", "playResponse");
        if (Authoriser.authorisePlayVideo(session)) {
            final User user = userRepository.getUser(session.getUsername());
            actionRepository.userStartedPlaybackVideo(user);


            String sdpOffer = receivedMessage.get("sdpOffer").getAsString();
            String mediaPath = receivedMessage.get("videoPath").getAsString();

            // create media pipeline
            final PlayMediaPipeline playMediaPipeline = new PlayMediaPipeline(kurento, mediaPath, session.getSession());
            playPipelines.put(session.getSessionId(), playMediaPipeline);

            // add error listener
            playMediaPipeline.getPlayer().addErrorListener(new EventListener<ErrorEvent>() {
                @Override
                public void onEvent(ErrorEvent errorEvent) {
                    System.out.println("Player error event...");
                    actionRepository.userEndedPlaybackVideo(user);
                    playMediaPipeline.sendPlayEnd(session.getSession());
                    playPipelines.remove(session.getSessionId());
                }
            });
            playMediaPipeline.getPlayer().addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
                @Override
                public void onEvent(EndOfStreamEvent endOfStreamEvent) {
                    System.out.println("Player end of stream event...");
                    actionRepository.userEndedPlaybackVideo(user);
                    playMediaPipeline.sendPlayEnd(session.getSession());
                    playPipelines.remove(session.getSessionId());
                }
            });

            playMediaPipeline.getWebRtc().addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.addProperty("for", SEND_ICE_FOR_PLAY);
                    response.add("candidate", JsonUtils
                            .toJsonObject(event.getCandidate()));
                    synchronized (session) {
                        try {
                            session.sendMessage(response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // get video info and send it to the user
            playMediaPipeline.getWebRtc().addMediaStateChangedListener(new EventListener<MediaStateChangedEvent>() {
                @Override
                public void onEvent(MediaStateChangedEvent mediaStateChangedEvent) {
                    System.out.println("Media state changed event...");
                    VideoInfo videoInfo = playMediaPipeline.getPlayer().getVideoInfo();

                    JsonObject response = new JsonObject();
                    response.addProperty("id", "videoInfo");
                    response.addProperty("isSeekable", videoInfo.getIsSeekable());
                    response.addProperty("initSeekable", videoInfo.getSeekableInit());
                    response.addProperty("endSeekable", videoInfo.getSeekableEnd());
                    response.addProperty("videoDuration", videoInfo.getDuration());

                    synchronized (session) {
                        try {
                            session.sendMessage(response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // SDP negotiation
            String sdpAnswer = playMediaPipeline.generateSdpAnswer(sdpOffer);

            response.addProperty("response", "accepted");
            response.addProperty("sdpAnswer", sdpAnswer);

            // play
            playMediaPipeline.play();

            synchronized (session) {
                session.sendMessage(response);
            }

            // gather candidates
            playMediaPipeline.getWebRtc().gatherCandidates();

        } else {
            response.addProperty("response", "rejected");
            response.addProperty("reason", "notAuthorised");
            session.sendMessage(response);
        }
    }

    /**
     * This method handles the pause request made by the user.
     * In this case peers are still connected, method tells play media pipeline to pause the video transmission.
     *
     * @param user is the user who requested the pause of the playback
     */
    public void handlePauseVideoMessage(UserSession user) {
        PlayMediaPipeline playMediaPipeline = playPipelines.get(user.getSessionId());
        if (playMediaPipeline == null)
            return;
        playMediaPipeline.getPlayer().pause();
    }

    /**
     * This method is used to resume the video playback, as the user requested it.
     * Method tells play media pipeline to resume video streaming. It sends again the video description parameters, see
     * {@link VideoInfo}
     *
     * @param user is the user who requested playback resume.
     */
    public void handleResumeVideoMessage(UserSession user) throws IOException {
        PlayMediaPipeline playMediaPipeline = playPipelines.get(user.getSessionId());
        if (playMediaPipeline == null)
            return;
        playMediaPipeline.getPlayer().play();

        VideoInfo videoInfo = playMediaPipeline.getPlayer().getVideoInfo();

        JsonObject response = new JsonObject();
        response.addProperty("id", "videoInfo");
        response.addProperty("isSeekable", videoInfo.getIsSeekable());
        response.addProperty("initSeekable", videoInfo.getSeekableInit());
        response.addProperty("endSeekable", videoInfo.getSeekableEnd());
        response.addProperty("videoDuration", videoInfo.getDuration());

        user.sendMessage(response);
    }

    /**
     * Method handle the seek video request. Tells the play media pipeline to go to the requested position.
     * If the seek fails, the server sends an information message to the user.
     *
     * @param user    is the user who requested the reposition
     * @param message is the message which must contain the new position.
     */
    public void handleSeekVideoMessage(UserSession user, JsonObject message) throws IOException {
        PlayMediaPipeline playMediaPipeline = playPipelines.get(user.getSessionId());
        try {
            long position = message.get("position").getAsLong();
            playMediaPipeline.getPlayer().setPosition(position);
        } catch (KurentoException e) {
            log.debug("The seek cannot be performed");
            JsonObject response = new JsonObject();
            response.addProperty("id", "seek");
            response.addProperty("message", "Seek failed");
            user.sendMessage(response);
        }

    }

    /**
     * Method used to handle the current position request.
     * The application server send an response as following:
     * <ul>
     *     <li> id: getPositionResponse</li>
     *     <li> position: <currentPosition></li>
     * </ul>
     *
     * @param user is the user who requested the current position in video.
     */
    public void handleGetVideoPositionMessage(UserSession user) throws IOException {
        PlayMediaPipeline playMediaPipeline = playPipelines.get(user.getSessionId());
        if (playMediaPipeline != null && !playMediaPipeline.isStreamEnded()) {
            long currentPosition = playMediaPipeline.getPlayer().getPosition();

            JsonObject response = new JsonObject();
            response.addProperty("id", "getPositionResponse");
            response.addProperty("position", currentPosition);

            user.sendMessage(response);
        }
    }

    /**
     * This method is used to handle stop video request.
     *
     * @param userSession is the user who requested to stop video playback.
     */
    public void handleStopVideoMessage(UserSession userSession) {
        User user = userRepository.getUser(userSession.getUsername());
        actionRepository.userEndedPlaybackVideo(user);

        PlayMediaPipeline playMediaPipeline = playPipelines.get(userSession.getSessionId());
        playMediaPipeline.getPlayer().stop();
    }
    //endregion

    //region Recording handlers
    /**
     * This method creates environment for recording the media streaming coming from user
     * It follow the forwarding steps:
     * <ul>
     *     <li> Creates the media logic (recording end point)</li>
     *     <li> sdp negotiation</li>
     *     <li> gathers ice candidates</li>
     *     <li> sends the response to the client (sdp answer)</li>
     * </ul>
     *
     * @param jsonMessage is the message coming from the client
     * @param caller      is the session involved in the communication
     */
    private void startRec(final UserSession caller, final JsonObject jsonMessage) throws IOException {
        String from = jsonMessage.get("from").getAsString();
        String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();

        // media logic
        RecordMediaPipeline recordMediaPipeline = new RecordMediaPipeline(kurento, from);

        recordPipeline.put(caller.getSessionId(), recordMediaPipeline);

        caller.setRecordMediaPipeline(recordMediaPipeline);

        // sdp negotiating
        String sdpAnswer = recordMediaPipeline.generateSdpAnswerFromRecordingEp(sdpOffer);

        recordMediaPipeline.getRecordingWebRtcEndpoint().addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.addProperty("for", "recording");
                response.add("candidate", JsonUtils
                        .toJsonObject(event.getCandidate()));
                try {
                    synchronized (caller.getSession()) {
                        caller.getSession().sendMessage(new TextMessage(response.toString()));
                    }
                } catch (IOException e) {
                    log.debug(e.getMessage());
                }
            }
        });

        JsonObject response = new JsonObject();
        response.addProperty("id", "startResponse");
        response.addProperty("response", "accepted");
        response.addProperty("sdpAnswer", sdpAnswer);

        synchronized (caller) {
            caller.sendMessage(response);
        }

        recordMediaPipeline.getRecordingWebRtcEndpoint().gatherCandidates();

        recordMediaPipeline.getRecordingWebRtcEndpoint().addMediaFlowInStateChangeListener(new EventListener<MediaFlowInStateChangeEvent>() {
            @Override
            public void onEvent(MediaFlowInStateChangeEvent mediaFlowInStateChangeEvent) {
                System.out.println("Media flow incoming");
                recordMediaPipeline.record();
            }
        });

        recordMediaPipeline.getRecordingWebRtcEndpoint().addMediaFlowOutStateChangeListener(mediaFlowOutStateChangeEvent -> {
            System.out.println("Media out listener!");
            recordMediaPipeline.record();
        });


        User user = userRepository.getUser(caller.getUsername());
        actionRepository.userStartedRecordingSession(user);
        Video video = new Video(recordMediaPipeline.getRecordingPath(), user, new Date());
        videoRepository.addVideo(video);
    }

    /***/
    private void stopRec(UserSession userSession, JsonObject jsonObject) {
        RecordMediaPipeline pipeline = recordPipeline.get(userSession.getSessionId());
        User user = userRepository.getUser(userSession.getUsername());
        actionRepository.userEndedRecordingSession(user);

        pipeline.release();
        //todo: notify observers about stopping the live stream

    }
    //endregion

    //region Live video handlers
    /**
     * todo: not working
     */
    private void startLiveVideo(String from, String sdpOffer, UserSession requestingUser) throws IOException {
        UserSession userRecording = registry.getByName(from);
        RecordMediaPipeline recordMediaPipeline = recordPipeline.get(userRecording.getSessionId());

        recordMediaPipeline.addLiveWatcher(requestingUser);
        requestingUser.setRecordMediaPipeline(recordMediaPipeline);

        // sdp negotiation
        String sdpAnswer = recordMediaPipeline.getWebRtcEpOfSubscriber(requestingUser).processOffer(sdpOffer);

        recordMediaPipeline.getWebRtcEpOfSubscriber(requestingUser).addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.addProperty("for", SEND_ICE_FOR_LIVE);
                response.add("candidate", JsonUtils
                        .toJsonObject(event.getCandidate()));
                try {
                    synchronized (requestingUser.getSession()) {
                        requestingUser.getSession().sendMessage(new TextMessage(response.toString()));
                    }
                } catch (IOException e) {
                    log.debug(e.getMessage());
                }
            }
        });

        JsonObject response = new JsonObject();
        response.addProperty("id", "liveResponse");
        response.addProperty("response", "accepted");
        response.addProperty("sdpAnswer", sdpAnswer);

        synchronized (requestingUser.getSession()) {
            System.out.println("Sending sdp ans message to " + requestingUser.getUsername());
            requestingUser.sendMessage(response);
        }

        recordMediaPipeline.getWebRtcEpOfSubscriber(requestingUser).gatherCandidates();
    }
    //endregion

    //region Requests handlers

    /***/
    private void getAllUsers(final WebSocketSession session) throws IOException {
        List<User> users = userRepository.getAllUsers();

        JsonObject response = new JsonObject();
        response.addProperty("id", "list_users_response");
        response.addProperty("users", gson.toJson(users));

        synchronized (session) {
            session.sendMessage(new TextMessage(response.toString()));
        }
    }

    private void getAllUsersWithUserRole(final WebSocketSession session) throws IOException {
        List<User> users = userRepository.getAllUsersByRole(Role.USER);

        JsonObject response = new JsonObject();
        response.addProperty("id", "list_users_response_USER");
        response.addProperty("users", gson.toJson(users));

        synchronized (session) {
            session.sendMessage(new TextMessage(response.toString()));
        }
    }

    /**
     * Sends a list with all users or with online user. Only a user with admin role can request a user list.
     *
     * @param user            is the user which requested the list.
     * @param receivedMessage is the message received by the application server
     */
    public void handleListUsersRequest(UserSession user, JsonObject receivedMessage) throws IOException {
        if (Authoriser.authoriseListUsers(user)) {
            System.out.println("List users event: " + receivedMessage.toString());

            String type = receivedMessage.get("type").getAsString();
            switch (type) {
                case "all":
                    getAllUsers(user.getSession());
                    break;

                case "userRole":
                    getAllUsersWithUserRole(user.getSession());
                    break;

                default:
                    //todo: log security conflict and disconnect user
            }

        } else {
            //todo: Log security and disconnect user
        }
    }

    /**
     * Sends the list of recorded videos by certain user. This request can be made only by a user who has admin role.
     * {@link Role}
     *
     * @param userSession is the user that requested the list
     * @param message     is the message received from user by application server
     */
    private void handleRecordedVideoListRequest(final UserSession userSession, JsonObject message) throws IOException {
        if (Authoriser.authoriseListRecordedVideos(userSession)) {
            String forUser = message.get("forUser").getAsString();
            User user = userRepository.getUser(forUser);

            List<Video> videos = videoRepository.getListVideoForUser(user.getId());

            JsonObject response = new JsonObject();
            response.addProperty("id", "requestListVideoResponse");
            response.addProperty("forUser", forUser);
            response.addProperty("videos", gson.toJson(videos));

            synchronized (userSession) {
                userSession.sendMessage(response);
            }
        }
    }

    /**
     * Send the actions made by certain user.
     * Application server receives a message that should contain:
     * <ul>
     *     <li> id: requestTimeline</li>
     *     <li> forUser: <username></li>
     *     <li> date: <date></li>
     * </ul>
     * Application server sends back a message that should contain:
     * <ul>
     *     <li> id: requestTimelineResponse</li>
     *     <li> forUser: <username></li>
     *     <li> actions: <actions list as a json array></li>
     * </ul>
     *
     * @param userSession is the user that has requested the action list
     * @param message     is the message received by the application server
     */
    public void handleTimelineRequest(final UserSession userSession, JsonObject message) throws IOException {
        if (Authoriser.authoriseListTimeline(userSession)) {
            // target user
            String forUser = message.get("forUser").getAsString();
            String dateString = message.get("date").getAsString();
            User user = userRepository.getUser(forUser);

            List<Action> actions = actionRepository.getTimeLineForUserOnDate(user, dateString);

            JsonObject response = new JsonObject();
            response.addProperty("id", "requestTimelineResponse");
            response.addProperty("forUser", forUser);
            response.addProperty("actions", gson.toJson(actions));

            synchronized (userSession) {
                userSession.sendMessage(response);
            }
        }
    }
    //endregion

    private void handleErrorResponse(Throwable throwable, final WebSocketSession session, String responseId)
            throws IOException {
//        stop(session);
        log.error(throwable.getMessage(), throwable);
        System.out.println("Error!: " + throwable.getMessage());
        JsonObject response = new JsonObject();

        response.addProperty("id", responseId);
        response.addProperty("response", "rejected");
        response.addProperty("message", throwable.getMessage());

        session.sendMessage(new TextMessage(response.toString()));
    }

    /**
     * Register new user in application server database
     *
     * @param session     is the session implied in the process, it should be an ADMIN
     * @param jsonMessage is the message from the client with user data
     * @see Role
     */
    private void enroll(final WebSocketSession session, JsonObject jsonMessage) throws IOException {
        String userJson = jsonMessage.get("user").getAsString();
        if (userJson != null) {
            User userModel = User.fromJson(userJson);
            //encode password
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            userModel.setPassword(encoder.encode(userModel.getPassword()));
            userRepository.addUser(userModel);

            JsonObject response = new JsonObject();
            response.addProperty("id", "enrollResponse");
            response.addProperty("response", "enrollSuccess");

            session.sendMessage(new TextMessage(response.toString()));
        }
    }

    /***/
    private void updateUser(UserSession userSession, JsonObject message){
        if(Authoriser.authoriseEditUser(userSession)){
            User userTarget = User.fromJson(message.get("user").getAsString());
            User user = userRepository.getUser(userSession.getUsername());
            actionRepository.onEditUser(user, userTarget);
            int i = userRepository.updateUser(userTarget);
            System.out.println("Updated rows: " + i);
        }
    }

    /**
     * This method is used after connection is closed
     */
    private void stop(final WebSocketSession session) throws IOException {
        UserSession stopperUser = registry.getBySession(session);
        if (stopperUser != null) {
            stopperUser.release();
        }
    }


}