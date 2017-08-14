package org.appspot.apprtc;

public class Y3kAppRtcRoomParams {
    public boolean isOrdered = true;
    public int maxRetransmitTimeMs = -1;
    public int maxRetransmits = -1;
    public String protocol = "";
    public boolean isNegotiated = false;

    public int channelIdAppRtcData = -1;
    public int channelIdManage = -1;
    public int channelIdMessageProxy = -1;

    public boolean isIosRemote = false;
    public AppRTCServer appRTCServer = AppRTCServer.APPRTC;

    public enum AppRTCServer {
        ASKEY("Askey Cloud Team", "https://webrtc-apcs.askeycloudapi.com", false), APPRTC("Appr.tc", "https://appr.tc", true);
        public final String displayName;
        public final String address;
        public final boolean useCustomStunServers;

        AppRTCServer(String displayName, String address, boolean useCustomStunServers) {
            this.displayName = displayName;
            this.address = address;
            this.useCustomStunServers = useCustomStunServers;
        }
    }
}
