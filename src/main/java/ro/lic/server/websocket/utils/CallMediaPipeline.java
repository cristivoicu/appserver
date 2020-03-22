package ro.lic.server.websocket.utils;

import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CallMediaPipeline {

    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");
    public static final String RECORDING_PATH = "file:///tmp/" + df.format(new Date()) + "-";
    public static final String RECORDING_EXT = ".webm";

    private final MediaPipeline pipeline;
    private final WebRtcEndpoint webRtcCaller;
    private final WebRtcEndpoint webRtcCallee;
    private final RecorderEndpoint recorderCaller;
    private final RecorderEndpoint recorderCallee;

    public CallMediaPipeline(KurentoClient kurento, String from, String to) {

        // Media pipeline
        pipeline = kurento.createMediaPipeline();

        // Media Elements (WebRtcEndpoint, RecorderEndpoint)
        webRtcCaller = new WebRtcEndpoint.Builder(pipeline).build();
        webRtcCallee = new WebRtcEndpoint.Builder(pipeline).build();

        recorderCaller = new RecorderEndpoint.Builder(pipeline, RECORDING_PATH + from + RECORDING_EXT)
                .build();
        recorderCallee = new RecorderEndpoint.Builder(pipeline, RECORDING_PATH + to + RECORDING_EXT)
                .build();

        // Connections
        webRtcCaller.connect(webRtcCallee);
        webRtcCaller.connect(recorderCaller);

        webRtcCallee.connect(webRtcCaller);
        webRtcCallee.connect(recorderCallee);
    }

    public void record() {
        recorderCaller.record();
        recorderCallee.record();
    }

    public String generateSdpAnswerForCaller(String sdpOffer) {
        return webRtcCaller.processOffer(sdpOffer);
    }

    public String generateSdpAnswerForCallee(String sdpOffer) {
        return webRtcCallee.processOffer(sdpOffer);
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public WebRtcEndpoint getCallerWebRtcEp() {
        return webRtcCaller;
    }

    public WebRtcEndpoint getCalleeWebRtcEp() {
        return webRtcCallee;
    }
}
