package ro.lic.server.constants;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Constants {
    public static final String KMS_WS_URI_PROP = "kms.url";
    public static final String KMS_WS_URI_DEFAULT = "ws://192.168.216.141:8888/kurento";
    public static final String KMS_WSS_URI_DEFAULT = "wss://192.168.216.141:8433/kurento";

    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");
}