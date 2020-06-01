package ro.lic.server.websocket.utils.pipeline;

import org.kurento.client.*;
import ro.lic.server.websocket.utils.UserSession;

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

    private final MediaPipeline mMediaPipeline;
    private final WebRtcEndpoint recordingWebRtcEndpoint;
    private final WebRtcEndpoint liveWatchWebRtcEndpoint;
    private final RecorderEndpoint recorderEndpoint;

    private boolean isStreaming = false;

    private final Map<String, WebRtcEndpoint> liveWatchers; //outgoings

    private HubPort mHubPort;

    public RecordMediaPipeline(KurentoClient kurentoClient, String from) {
        // create recording path
        recordingPath = String.format("file:///home/kurento/UsersVideos/%s__%s%s", dateFormat.format(new Date()), from, RECORDING_EXT_WEBM);
        // create media pipeline
        mMediaPipeline = kurentoClient.createMediaPipeline();

        // create endpoints
        recordingWebRtcEndpoint = new WebRtcEndpoint.Builder(mMediaPipeline).build();
        liveWatchWebRtcEndpoint = new WebRtcEndpoint.Builder(mMediaPipeline).build();
        // setting the max bandwidth to 2.5 mbs (full hd capable)
        recordingWebRtcEndpoint.setMaxVideoRecvBandwidth(3000, new Continuation<Void>() {
            @Override
            public void onSuccess(Void aVoid) throws Exception {
                System.out.println("Set max video recv. bandwidth");
            }

            @Override
            public void onError(Throwable throwable) throws Exception {
                System.out.println("Failed to set max video recv band");
            }
        }); // unit kbps (set it to 2.5 mbs)
        recordingWebRtcEndpoint.setMaxVideoSendBandwidth(3000, new Continuation<Void>() {
            @Override
            public void onSuccess(Void aVoid) throws Exception {
                System.out.println("Set max video send bandwidth");
            }

            @Override
            public void onError(Throwable throwable) throws Exception {
                System.out.println("Failed to set max video recv. band");
            }
        });
        recordingWebRtcEndpoint.setMaxAudioRecvBandwidth(520, new Continuation<Void>() {
            @Override
            public void onSuccess(Void aVoid) throws Exception {
                System.out.println("Set max audio recv band");
            }

            @Override
            public void onError(Throwable throwable) throws Exception {
                System.out.println("Failed to set max audio recv band");
                //todo change resolution on user!!
            }
        });

        recordingWebRtcEndpoint.addErrorListener(errorEvent -> {
            System.out.println("ERROR: " + errorEvent.getDescription());
        });

        liveWatchWebRtcEndpoint.setMaxVideoRecvBandwidth(3000);
        liveWatchWebRtcEndpoint.setMaxVideoSendBandwidth(3000);
        liveWatchWebRtcEndpoint.setMaxOutputBitrate(3000);

        recorderEndpoint = new RecorderEndpoint.Builder(mMediaPipeline, recordingPath)
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
        liveWatchers = new ConcurrentHashMap<>();
        recordingWebRtcEndpoint.connect(recorderEndpoint);

        DispatcherOneToMany dispatcherOneToMany = new DispatcherOneToMany.Builder(mMediaPipeline).build();
        mHubPort = new HubPort.Builder(dispatcherOneToMany).build();
        mHubPort.setMaxOutputBitrate(3000);
        recordingWebRtcEndpoint.connect(mHubPort);

        dispatcherOneToMany.setSource(mHubPort);

        isStreaming = true;
    }

    /**
     * Subscribe user to the recording session
     *
     * @param session is the subscriber user session
     */
    public void addLiveWatcher(UserSession session) {
        WebRtcEndpoint liveWatcherWebRtcEndPoint = new WebRtcEndpoint.Builder(mMediaPipeline).build();

        liveWatcherWebRtcEndPoint.setMaxVideoSendBandwidth(3000);
        liveWatcherWebRtcEndPoint.setMaxVideoRecvBandwidth(3000);
        liveWatcherWebRtcEndPoint.setMaxOutputBitrate(3000);

        mHubPort.connect(liveWatcherWebRtcEndPoint);
        //recordingWebRtcEndpoint.connect(liveWatcherWebRtcEndPoint);

        liveWatchers.put(session.getSessionId(), liveWatcherWebRtcEndPoint);
    }

    /**
     * Unsubscribe user to the recording session
     *
     * @param session is the subscriber user session
     */
    public void unsubscribe(UserSession session) {
        WebRtcEndpoint subscriberWebRtcEp = liveWatchers.get(session.getSessionId());
        //recordingWebRtcEndpoint.disconnect(subscriberWebRtcEp);
        mHubPort.disconnect(subscriberWebRtcEp);

        liveWatchers.remove(session.getSessionId());
    }

    public WebRtcEndpoint getWebRtcEpOfSubscriber(UserSession session){
        return liveWatchers.get(session.getSessionId());
    }

    public void record() {
        recorderEndpoint.record();
    }

    public String generateSdpAnswerFromRecordingEp(String sdpOffer) {
        return recordingWebRtcEndpoint.processOffer(sdpOffer);
    }

    public void addLiveCandidate(IceCandidate iceCandidate, UserSession user){
        liveWatchers.get(user.getSessionId()).addIceCandidate(iceCandidate);
    }

    public void addCandidate(IceCandidate iceCandidate) {
        recordingWebRtcEndpoint.addIceCandidate(iceCandidate);
    }

    public void release() {
        if(isStreaming) {
            recorderEndpoint.stopAndWait();
            isStreaming = false;
        }
        recordingWebRtcEndpoint.release();
        recorderEndpoint.release();
        mMediaPipeline.release();

        //todo: release the subscriber endpoints
    }

    //region Getters and setters

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
