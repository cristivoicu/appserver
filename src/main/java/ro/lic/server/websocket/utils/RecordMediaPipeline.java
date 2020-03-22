package ro.lic.server.websocket.utils;

import org.kurento.client.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Encapsulate a media pipeline needed by record the media stream received from client
 */
public class RecordMediaPipeline {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss");
    private static final String RECORDING_EXT_WEBM = ".webm";
    private final String recordingPath;

    private final MediaPipeline mediaPipeline;
    private final WebRtcEndpoint webRtcEndpoint;
    private final RecorderEndpoint recorderEndpoint;

    public RecordMediaPipeline(KurentoClient kurentoClient, String from){
        // create recording path
        recordingPath = String.format("file:///home/kurento/UsersVideos/%s__%s%s", dateFormat.format(new Date()), from, RECORDING_EXT_WEBM);
        // create media pipeline
        mediaPipeline = kurentoClient.createMediaPipeline();
        // create endpoints
        webRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
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
        webRtcEndpoint.connect(recorderEndpoint);
        //webRtcEndpoint.connect(recorderEndpoint, MediaType.VIDEO);
    }

    public void record(){
        recorderEndpoint.record();
    }

    public String generateSdpAnswerFromCaller(String sdpOffer){
        return webRtcEndpoint.processOffer(sdpOffer);
    }

    public void addCandidate(IceCandidate iceCandidate){
        webRtcEndpoint.addIceCandidate(iceCandidate);
    }

    public void release(){
        webRtcEndpoint.release();
        recorderEndpoint.stopAndWait();
        recorderEndpoint.stop();
        recorderEndpoint.release();
        mediaPipeline.release();
    }

    //region Getters and setters

    public MediaPipeline getMediaPipeline() {
        return mediaPipeline;
    }

    public WebRtcEndpoint getWebRtcEndpoint() {
        return webRtcEndpoint;
    }

    public RecorderEndpoint getRecorderEndpoint() {
        return recorderEndpoint;
    }

    public String getRecordingPath() {
        return recordingPath;
    }

    //endregion


}
