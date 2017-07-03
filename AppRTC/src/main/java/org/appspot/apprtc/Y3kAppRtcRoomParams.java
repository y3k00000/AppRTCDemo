package org.appspot.apprtc;

public class Y3kAppRtcRoomParams {
    public static boolean isOrdered = true;
    public static int maxRetransmitTimeMs = -1;
    public static int maxRetransmits = -1;
    public static String protocol = "";
    public static boolean isNegotiated = false;

    public static int channelIdAppRtcData = -1;
    public static int channelIdManage = -1;
    public static int channelIdMessageProxy = -1;

    public static boolean isIosRemote = false;
    public static AppRTCServer appRTCServer = AppRTCServer.APPRTC;

    public enum AppRTCServer {
        ASKEY("Askey Cloud Team", "https://webrtc-apcs.askeycloudapi.com", false), APPRTC("Appr.tc", "https://appr.tc", true);
        public final String displayName;
        public final String address;
        public final boolean useCustomStunStevers;

        AppRTCServer(String displayName, String address, boolean useCustomStunStevers) {
            this.displayName = displayName;
            this.address = address;
            this.useCustomStunStevers = useCustomStunStevers;
        }
    }
}
