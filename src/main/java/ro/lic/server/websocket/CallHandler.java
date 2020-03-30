package ro.lic.server.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ro.lic.server.model.Roles;
import ro.lic.server.model.repository.UserRepository;
import ro.lic.server.model.repository.VideoRepository;
import ro.lic.server.model.tables.User;
import ro.lic.server.model.tables.Video;
import ro.lic.server.websocket.security.Authoriser;
import ro.lic.server.websocket.utils.*;

import java.io.IOException;
import java.nio.ByteBuffer;
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

    @Autowired(required = true)
    private KurentoClient kurento;

    @Autowired(required = true)
    private UserRegistry registry = new UserRegistry();

    @Override
    public void afterConnectionClosed(final WebSocketSession session, CloseStatus status) throws Exception {
        String name = registry.getBySession(session).getUsername();
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
        // check if user is already connected
        if (registry.exists(username)) {
            session.close(CloseStatus.SERVER_ERROR);
            // todo: set log error
        } else {
            System.out.println(String.format("User %s connected!", username));
            UserSession userSession = new UserSession(session, username, userRepository.getUserRoleByUsername(username));
            registry.register(userSession);
        }
        //super.afterConnectionEstablished(session);
        PingMessage pingMessage = new PingMessage();
        session.sendMessage(pingMessage);
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
                case EVENT_PLAY:
//                    play(user, jsonMessage);
                    break;
                case EVENT_ICE_CANDIDATE: {
                    System.out.println("on Ice" + receivedMessage.toString());
                    String iceFor = receivedMessage.get("for").getAsString();
                    JsonObject candidate = receivedMessage.get("candidate").getAsJsonObject();

                    IceCandidate cand =
                            new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid")
                                    .getAsString(), candidate.get("sdpMLineIndex").getAsInt());
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
                default:
                    System.out.println("default case");
                    break;
            }
        } catch (JsonSyntaxException e) {
            session.sendMessage(new TextMessage("json error"));
            System.out.println("!!! JSON FROM CLIENT ERROR: " + e.getMessage());
        }
    }

    /***/
    private void playVideo(UserSession session, JsonObject receivedMessage) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("id", "playResponse");
        if(Authoriser.authorisePlayVideo(session)){
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
                    playMediaPipeline.sendPlayEnd(session.getSession());
                }
            });
            playMediaPipeline.getPlayer().addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
                @Override
                public void onEvent(EndOfStreamEvent endOfStreamEvent) {
                    System.out.println("Player end of stream event...");
                    playMediaPipeline.sendPlayEnd(session.getSession());
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
                    synchronized (session){
                        try {
                            session.sendMessage(response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // get video info and send it to the user
/*            playMediaPipeline.getWebRtc().addMediaStateChangedListener(new EventListener<MediaStateChangedEvent>() {
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

                    synchronized (session){
                        try {
                            session.sendMessage(response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });*/

            // SDP negotiation
            String sdpAnswer = playMediaPipeline.generateSdpAnswer(sdpOffer);

            response.addProperty("response", "accepted");
            response.addProperty("sdpAnswer", sdpAnswer);

            // play
            playMediaPipeline.play();

            synchronized (session){
                session.sendMessage(response);
            }

            // gather candidates
            playMediaPipeline.getWebRtc().gatherCandidates();

        }else{
            response.addProperty("response", "rejected");
            response.addProperty("reason", "notAuthorised");
            session.sendMessage(response);
        }
    }

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
        Video video = new Video(recordMediaPipeline.getRecordingPath(), user, new Date());
        videoRepository.addVideo(video);
    }

    private void stopRec(UserSession userSession, JsonObject jsonObject) {
        RecordMediaPipeline pipeline = recordPipeline.get(userSession.getSessionId());

        pipeline.release();
        //todo: notify observers about stopping the live stream

    }

    /** */
    private void startLiveVideo(String from, String sdpOffer, UserSession requestingUser) throws IOException {
        UserSession userRecording = registry.getByName(from);
        RecordMediaPipeline recordMediaPipeline = recordPipeline.get(userRecording.getSessionId());

        recordMediaPipeline.addSubscriber(requestingUser);
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

    private void getAllUsers(final WebSocketSession session) throws IOException {
        List<User> users = userRepository.getAllUsers();

        JsonObject response = new JsonObject();
        response.addProperty("id", "list_users_response");

        response.addProperty("users", gson.toJson(users));

        synchronized (session) {
            session.sendMessage(new TextMessage(response.toString()));
        }
    }
    public void handleListUsersRequest(UserSession user, JsonObject receivedMessage) throws IOException {
        if (Authoriser.authoriseListUsers(user)) {
            System.out.println("List users event: " + receivedMessage.toString());

            String type = receivedMessage.get("type").getAsString();
            switch (type) {
                case "all":
                    getAllUsers(user.getSession());
                    break;

                case "online":

                    break;

                default:
                    //todo: log security conflict and disconnect user
            }

        } else {
            //todo: Log security and disconnect user
        }
    }

    /***/
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
     * @see Roles
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

    /**
     * This method is used after connection is closed
     */
    private void stop(final WebSocketSession session) throws IOException {
        // Both users can stop the communication. A 'stopCommunication'
        // message will be sent to the other peer.
        UserSession stopperUser = registry.getBySession(session);
        if (stopperUser != null) {
            stopperUser.release();
        }
    }
/*    public void releasePipeline(final UserSession session) {
        String sessionId = session.getSessionId();

        if (playPipelines.containsKey(sessionId)) {
            playPipelines.get(sessionId).release();
            playPipelines.remove(sessionId);
        }
        session.setWebRtcEndpoint(null);
        session.setPlayingWebRtcEndpoint(null);

        // set to null the endpoint of the other user
        UserSession stoppedUser =
                (session.getCallingFrom() != null) ? registry.getByName(session.getCallingFrom())
                        : registry.getByName(session.getCallingTo());
        stoppedUser.setWebRtcEndpoint(null);
        stoppedUser.setPlayingWebRtcEndpoint(null);
    }*/

/*    private void play(final UserSession session, JsonObject jsonMessage) throws IOException {
        String user = jsonMessage.get("user").getAsString();
        log.debug("Playing recorded call of user '{}'", user);

        JsonObject response = new JsonObject();
        response.addProperty("id", "playResponse");

        if (registry.getByName(user) != null && registry.getBySession(session.getSession()) != null) {
            final PlayMediaPipeline playMediaPipeline =
                    new PlayMediaPipeline(kurento, user, session.getSession());

            session.setPlayingWebRtcEndpoint(playMediaPipeline.getWebRtc());

            playMediaPipeline.getPlayer().addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
                @Override
                public void onEvent(EndOfStreamEvent event) {
                    UserSession user = registry.getBySession(session.getSession());
                    releasePipeline(user);
                    playMediaPipeline.sendPlayEnd(session.getSession());
                }
            });

            playMediaPipeline.getWebRtc().addIceCandidateFoundListener(
                    new EventListener<IceCandidateFoundEvent>() {

                        @Override
                        public void onEvent(IceCandidateFoundEvent event) {
                            JsonObject response = new JsonObject();
                            response.addProperty("id", "iceCandidate");
                            response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                            try {
                                synchronized (session) {
                                    session.getSession().sendMessage(new TextMessage(response.toString()));
                                }
                            } catch (IOException e) {
                                log.debug(e.getMessage());
                            }
                        }
                    });

            String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
            String sdpAnswer = playMediaPipeline.generateSdpAnswer(sdpOffer);

            response.addProperty("response", "accepted");

            response.addProperty("sdpAnswer", sdpAnswer);

            playMediaPipeline.play();
            pipelines.put(session.getSessionId(), playMediaPipeline.getPipeline());
            synchronized (session.getSession()) {
                session.sendMessage(response);
            }

            playMediaPipeline.getWebRtc().gatherCandidates();

        } else {
            response.addProperty("response", "rejected");
            response.addProperty("error", "No recording for user '" + user
                    + "'. Please type a correct user in the 'Peer' field.");
            session.getSession().sendMessage(new TextMessage(response.toString()));
        }
    }*/

}