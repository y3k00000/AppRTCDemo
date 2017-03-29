package y3k.rtc.test;

import org.appspot.apprtc.WebSocketChannelClient;
import org.appspot.apprtc.WebSocketRTCClient;

public class LameWebSocketRTCClient extends WebSocketRTCClient {
    final WebSocketChannelClient.WebSocketChannelEvents webSocketCallback;
    public LameWebSocketRTCClient(SignalingEvents eventsCallBack, WebSocketChannelClient.WebSocketChannelEvents callback){
        super(eventsCallBack);
        this.webSocketCallback = callback;
    }

    @Override
    public void onWebSocketMessage(String msg) {
        if(this.webSocketCallback==null) {
            super.onWebSocketMessage(msg);
        } else{
            this.webSocketCallback.onWebSocketMessage(msg);
        }
    }

    @Override
    public void onWebSocketClose() {
        if(this.webSocketCallback==null) {
            super.onWebSocketClose();
        } else{
            this.webSocketCallback.onWebSocketClose();
        }
    }

    @Override
    public void onWebSocketError(String description) {
        if(this.webSocketCallback==null) {
            super.onWebSocketError(description);
        } else{
            this.webSocketCallback.onWebSocketError(description);
        }
    }

    @Override
    public void onBinaryMessage(byte[] binary) {
        if(this.webSocketCallback==null) {
            super.onBinaryMessage(binary);
        } else{
            this.webSocketCallback.onBinaryMessage(binary);
        }
    }
}
