package ro.lic.server.websocket.utils.pipeline;

import org.kurento.client.*;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HubMediaPipeline {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss");
    private static final String RECORDING_EXT_WEBM = ".webm";

    private HubPort mHubPort;
    private Composite mComposite;
    private MediaPipeline mMediaPipeline;

    private final Map<String, WebRtcEndpoint> liveWatchers = new ConcurrentHashMap<>();
    private final Map<String, WebRtcEndpoint> liveStreamers = new ConcurrentHashMap<>();
    private final Map<String, RecorderEndpoint> liveStreamersRecorders = new ConcurrentHashMap<>();

    public HubMediaPipeline(KurentoClient kurentoClient){
        mMediaPipeline = kurentoClient.createMediaPipeline();
        mComposite = new Composite.Builder(mMediaPipeline).build();

        mHubPort = new HubPort.Builder(mComposite).build();
        mHubPort.setMaxOutputBitrate(3000);
    }

    public void addLiveWatcher(String username){
        WebRtcEndpoint webRtcEndpoint =  new WebRtcEndpoint.Builder(mMediaPipeline).build();
        webRtcEndpoint.setName(username);
        webRtcEndpoint.setMaxOutputBitrate(3000);
        webRtcEndpoint.setMaxVideoRecvBandwidth(3000);

        mHubPort.connect(webRtcEndpoint);
        liveWatchers.put(username, webRtcEndpoint);
    }

    public void removeLiveWatcher(String username){
        WebRtcEndpoint webRtcEndpoint = liveWatchers.get(username);
        liveWatchers.remove(username);
        mHubPort.disconnect(webRtcEndpoint);
    }

    public void addIceCandidateLiveStreamer(IceCandidate candidate, String username){
        liveStreamers.get(username).addIceCandidate(candidate);
    }

    public void addIceCandidateLiveWatcher(IceCandidate candidate, String username){
        liveWatchers.get(username).addIceCandidate(candidate);
    }

    public String addLiveStreamer(String username){
        WebRtcEndpoint webRtcEndpoint =  new WebRtcEndpoint.Builder(mMediaPipeline).build();
        webRtcEndpoint.setName(username);
        webRtcEndpoint.setMaxOutputBitrate(3000);
        webRtcEndpoint.setMaxVideoRecvBandwidth(3000);

        String recordingPath = String.format("file:///home/kurento/UsersVideos/%s__%s%s",
                dateFormat.format(new Date()),
                username,
                RECORDING_EXT_WEBM);

        RecorderEndpoint recorderEndpoint = new RecorderEndpoint.Builder(mMediaPipeline, recordingPath)
                .withMediaProfile(MediaProfileSpecType.WEBM)
                .build();

        recorderEndpoint.setMaxOutputBitrate(3000);
        recorderEndpoint.setMinOutputBitrate(3000);


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

        webRtcEndpoint.connect(mHubPort);
        webRtcEndpoint.connect(recorderEndpoint);
        liveStreamers.put(username, webRtcEndpoint);
        liveStreamersRecorders.put(username, recorderEndpoint);

        return recordingPath;
    }

    public void startRecording(String unsername){
        liveStreamersRecorders.get(unsername).record();
    }

    public void removeLiveStreamer(String username){
        WebRtcEndpoint webRtcEndpoint = liveStreamers.get(username);
        liveStreamers.remove(username);
        mHubPort.disconnect(webRtcEndpoint);
    }

    public String getSdpAnswerForLiveWatcher(String username, String sdpOffer){
        return liveWatchers.get(username).processOffer(sdpOffer);
    }

    public String getSdpAnswerForLiveStreamer(String username, String sdpOffer){
        return liveStreamers.get(username).processOffer(sdpOffer);
    }

    public WebRtcEndpoint getLiveWatcherWebRtcEndpoint(String username){
        return liveWatchers.get(username);
    }

    public WebRtcEndpoint getLiveStreamerWebRtcEndpoint(String username){
        return liveStreamers.get(username);
    }
}
