package ro.lic.server.constants;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Constants {
    public static final String KMS_WS_URI_PROP = "kms.url";
    public static final String KMS_WS_URI_DEFAULT = "ws://192.168.216.129:8888/kurento";

    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");

    //public static final String RECORDING_PATH = "/home/kurento/Desktop/" + df.format(new Date()) + "-";
    public static final String RECORDING_PATH = "file:///home/kurento/Desktop/" + df.format(new Date()) + "-";
    public static final String RECORDING_EXT = ".mp4";

    public static final String RECORDING_STATIC_PATH = "file:///tmp/2020-03-03_23-26-11-15-mimi.webm";
}
