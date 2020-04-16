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

    private final MediaPipeline mediaPipeline;
    private final WebRtcEndpoint recordingWebRtcEndpoint;
    private final WebRtcEndpoint liveWatchWebRtcEndpoint;
    private final RecorderEndpoint recorderEndpoint;

    private final Map<String, WebRtcEndpoint> liveWatchers; //outgoings

    public RecordMediaPipeline(KurentoClient kurentoClient, String from) {
        // create recording path
        recordingPath = String.format("file:///home/kurento/UsersVideos/%s__%s%s", dateFormat.format(new Date()), from, RECORDING_EXT_WEBM);
        // create media pipeline
        mediaPipeline = kurentoClient.createMediaPipeline();

        // create endpoints
        recordingWebRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
        liveWatchWebRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
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

        liveWatchWebRtcEndpoint.setMaxVideoRecvBandwidth(2500);
        liveWatchWebRtcEndpoint.setMaxVideoSendBandwidth(2500);

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
        recordingWebRtcEndpoint.connect(liveWatchWebRtcEndpoint);

        liveWatchers = new ConcurrentHashMap<>();
    }

    /**
     * Subscribe user to the recording session
     *
     * @param session is the subscriber user session
     */
    public void addLiveWatcher(UserSession session) {
/*        WebRtcEndpoint liveWatcherWebRtcEndPoint = new WebRtcEndpoint.Builder(mediaPipeline).build();

        liveWatcherWebRtcEndPoint.setMaxVideoSendBandwidth(2500);
        liveWatcherWebRtcEndPoint.setMaxVideoRecvBandwidth(2500);

        recordingWebRtcEndpoint.connect(liveWatcherWebRtcEndPoint, new Continuation<Void>() {
            @Override
            public void onSuccess(Void aVoid) throws Exception {
                System.out.println("recording connected, success");
            }

            @Override
            public void onError(Throwable throwable) throws Exception {
                System.out.println("onError: failed to connect recording endpoint to live watcher.");
            }
        });


        liveWatchers.put(session.getSessionId(), liveWatcherWebRtcEndPoint);*/
    }

    /**
     * Unsubscribe user to the recording session
     *
     * @param session is the subscriber user session
     */
    public void unsubscribe(UserSession session) {
        WebRtcEndpoint subscriberWebRtcEp = liveWatchers.get(session.getSessionId());
        recordingWebRtcEndpoint.disconnect(subscriberWebRtcEp);

        liveWatchers.remove(session.getSessionId());
    }

    public WebRtcEndpoint getWebRtcEpOfSubscriber(UserSession session){
        /*return liveWatchers.get(session.getSessionId());*/
        return liveWatchWebRtcEndpoint;
    }

    public void record() {
        recorderEndpoint.record();
    }

    public String generateSdpAnswerFromRecordingEp(String sdpOffer) {
        return recordingWebRtcEndpoint.processOffer(sdpOffer);
    }

    public void addLiveCandidate(IceCandidate iceCandidate, UserSession user){
        /*liveWatchers.get(user.getSessionId()).addIceCandidate(iceCandidate);*/
        liveWatchWebRtcEndpoint.addIceCandidate(iceCandidate);
    }

    public void addCandidate(IceCandidate iceCandidate) {
        recordingWebRtcEndpoint.addIceCandidate(iceCandidate);
    }

    public void release() {
        recorderEndpoint.stopAndWait();
        recordingWebRtcEndpoint.release();
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
