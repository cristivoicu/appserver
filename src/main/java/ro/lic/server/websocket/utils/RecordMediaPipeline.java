package ro.lic.server.websocket.utils;

import org.kurento.client.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulate the media pipeline need to record videos from a user
 * Is also contains a map of webrtc endpoints needed to catch the live stream
 */
public class RecordMediaPipeline {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss");
    private static final String RECORDING_EXT_WEBM = ".webm";
    private final String recordingPath;

    private final MediaPipeline mediaPipeline;
    private final WebRtcEndpoint recordingWebRtcEndpoint;
    private final RecorderEndpoint recorderEndpoint;

    private final Map<String, WebRtcEndpoint> subscribers; //outgoings

    public RecordMediaPipeline(KurentoClient kurentoClient, String from) {
        // create recording path
        recordingPath = String.format("file:///home/kurento/UsersVideos/%s__%s%s", dateFormat.format(new Date()), from, RECORDING_EXT_WEBM);
        // create media pipeline
        mediaPipeline = kurentoClient.createMediaPipeline();
        // create endpoints
        recordingWebRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
        recorderEndpoint = new RecorderEndpoint.Builder(mediaPipeline, recordingPath)
                .withMediaProfile(MediaProfileSpecType.WEBM)
                .build();

        recorderEndpoint.addRecordingListener(new EventListener<RecordingEvent>() {
            @Override
            public void onEvent(RecordingEvent recordingEvent) {
                System.out.println("RECORDER: Recording event");
            }
        });

        recorderEndpoint.addPausedListener(new EventListener<PausedEvent>() {
            @Override
            public void onEvent(PausedEvent pausedEvent) {
                System.out.println("RECORDER: Paused event");
            }
        });

        recorderEndpoint.addStoppedListener(new EventListener<StoppedEvent>() {
            @Override
            public void onEvent(StoppedEvent stoppedEvent) {
                System.out.println("RECORDER: Stopped event");
            }
        });

        // connections
        recordingWebRtcEndpoint.connect(recorderEndpoint);
        //webRtcEndpoint.connect(recorderEndpoint, MediaType.VIDEO);

        subscribers = new ConcurrentHashMap<>();
    }

    /**
     * Subscribe user to the recording session
     *
     * @param session is the subscriber user session
     */
    public void addSubscriber(UserSession session) {
        WebRtcEndpoint subscriberWebRtcEp = new WebRtcEndpoint.Builder(mediaPipeline).build();
        recordingWebRtcEndpoint.connect(subscriberWebRtcEp);
        subscriberWebRtcEp.connect(recorderEndpoint);
        subscribers.put(session.getSessionId(), subscriberWebRtcEp);
    }

    /**
     * Unsubscribe user to the recording session
     *
     * @param session is the subscriber user session
     */
    public void unsubscribe(UserSession session) {
        WebRtcEndpoint subscriberWebRtcEp = subscribers.get(session.getSessionId());
        recordingWebRtcEndpoint.disconnect(subscriberWebRtcEp);

        subscribers.remove(session.getSessionId());
    }

    public WebRtcEndpoint getWebRtcEpOfSubscriber(UserSession session){
        return subscribers.get(session.getSessionId());
    }

    public void record() {
        recorderEndpoint.record();
    }

    public String generateSdpAnswerFromRecordingEp(String sdpOffer) {
        return recordingWebRtcEndpoint.processOffer(sdpOffer);
    }

    public void addLiveCandidate(IceCandidate iceCandidate, UserSession user){
        subscribers.get(user.getSessionId()).addIceCandidate(iceCandidate);
    }

    public void addCandidate(IceCandidate iceCandidate) {
        recordingWebRtcEndpoint.addIceCandidate(iceCandidate);
    }

    public void release() {
        recordingWebRtcEndpoint.release();
        recorderEndpoint.stopAndWait();
        recorderEndpoint.stop();
        recorderEndpoint.release();
        mediaPipeline.release();

        //todo: release the subscriber endpoints
    }

    //region Getters and setters

    public MediaPipeline getMediaPipeline() {
        return mediaPipeline;
    }

    public WebRtcEndpoint getRecordingWebRtcEndpoint() {
        return recordingWebRtcEndpoint;
    }

    public RecorderEndpoint getRecorderEndpoint() {
        return recorderEndpoint;
    }

    public String getRecordingPath() {
        return recordingPath;
    }

    //endregion


}
