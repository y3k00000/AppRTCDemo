/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package y3k.rtc.test;

import android.app.Activity;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.AppRTCClient.RoomConnectionParameters;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.PeerConnectionClient.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.WebSocketChannelClient;
import org.appspot.apprtc.WebSocketRTCClient;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Deprecated
public class Y3kAppRTCClient implements AppRTCClient.SignalingEvents,
        PeerConnectionClient.PeerConnectionEvents {

    private static final String TAG = Y3kAppRTCClient.class.getName();
    private final Activity activity;
    private WebSocketRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected = false;

    public enum Command{
        SENDING_START, SENDING_END
    }

    // Controls
    public Y3kAppRTCClient(Activity activity, WebSocketChannelClient.WebSocketChannelEvents webSocketCallback) {
        this.activity = activity;

        boolean loopback = false;
        boolean tracing = true;

        DataChannelParameters dataChannelParameters = new DataChannelParameters(
                true, //EXTRA_ORDERED
                -1, //EXTRA_MAX_RETRANSMITS_MS
                -1, //EXTRA_MAX_RETRANSMITS
                "", //EXTRA_PROTOCOL
                false,//EXTRA_NEGOTIATED,
                -1); //EXTRA_ID
        peerConnectionParameters =
                new PeerConnectionParameters(
                        false,//EXTRA_VIDEO_CALL,
                        loopback,
                        tracing,
                        0, // videoWidth
                        0, // videoHeight
                        0, //EXTRA_VIDEO_FPS
                        0, //EXTRA_VIDEO_BITRATE
                        "VP8", //EXTRA_VIDEOCODEC
                        true, //EXTRA_HWCODEC_ENABLED
                        false, //EXTRA_FLEXFEC_ENABLED
                        0, //EXTRA_AUDIO_BITRATE
                        "OPUS", //EXTRA_AUDIOCODEC
                        false, //EXTRA_NOAUDIOPROCESSING_ENABLED
                        false, //EXTRA_AECDUMP_ENABLED
                        false, //EXTRA_OPENSLES_ENABLED
                        false, //EXTRA_DISABLE_BUILT_IN_AEC
                        false, //EXTRA_DISABLE_BUILT_IN_AGC
                        false, //EXTRA_DISABLE_BUILT_IN_NS
                        false, //EXTRA_ENABLE_LEVEL_CONTROL,
                        dataChannelParameters);

        // Create connection client. Use DirectRTCClient if room name is an IP otherwise use the
        // standard WebSocketRTCClient.
//    if (loopback || !DirectRTCClient.IP_PATTERN.matcher(roomId).matches()) {
        appRtcClient = new LameWebSocketRTCClient(this, webSocketCallback);
//    } else {
//      Log.i(TAG, "Using DirectRTCClient because room name looks like an IP.");
//      appRtcClient = new DirectRTCClient(this);
//    }
        // Create connection parameters.
    }

    public Y3kAppRTCClient connect(String roomId) {
        roomConnectionParameters = new RoomConnectionParameters("https://appr.tc", roomId, false);
        appRtcClient.connectToRoom(roomConnectionParameters);
        return this;
    }

    public int postMessage(final String message) throws NetworkOnMainThreadException{
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(this.appRtcClient.getWsClient().getRoomPostURL()).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setReadTimeout(8000);
            httpURLConnection.setConnectTimeout(8000);
            httpURLConnection.getOutputStream().write(message.getBytes());
            return httpURLConnection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

//    public void sendMessage(final String message) {
//        appRtcClient.getWsClient().getHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                appRtcClient.getWsClient().send(message);
//            }
//        });
//    }
//
//    public void sendBinary(final byte[] binary) {
//        appRtcClient.getWsClient().getHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                appRtcClient.getWsClient().sendBinary(binary);
//            }
//        });
//    }

    public static byte[] DecodeHexStringToByteArray(String string) {
        int len = string.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(string.charAt(i), 16) << 4) + Character.digit(string.charAt(i+1), 16));
        }
        return data;
    }

    public static String encodeByteArrayToHexString(byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    public void disconnect() {
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
    }

    @Override
    public void onConnectedToRoom(final SignalingParameters params) {
        Log.d(TAG, "onConnectedToRoom(" + params.clientId + ")");
        // TODO onConnectedToRoom
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        Log.d(TAG, "onRemoteDescription(" + sdp.description + ")");
        // TODO onRemoteDescription
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        Log.d(TAG, "onRemoteIceCandidate()");
        // TODO onRemoteIceCandidate
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
        Log.d(TAG, "onRemoteIceCandidatesRemoved()");
        // TODO onRemoteIceCandidatesRemoved
    }

    @Override
    public void onChannelClose() {
        Log.d(TAG, "onChannelClose()");
        disconnect();
    }

    @Override
    public void onChannelError(final String description) {
        Log.d(TAG, "onChannelError(" + description + ")");
        // TODO : onChannelError
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        Log.d(TAG, "onLocalDescription(" + sdp.description + ")");
        if (appRtcClient != null) {
            if (signalingParameters.initiator) {
                appRtcClient.sendOfferSdp(sdp);
            } else {
                appRtcClient.sendAnswerSdp(sdp);
            }
        }
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        Log.d(TAG, "onIceCandidate(" + candidate.toString() + ")");
        if (appRtcClient != null) {
            appRtcClient.sendLocalIceCandidate(candidate);
        }
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        Log.d(TAG, "onIceCandidatesRemoved()");
        if (appRtcClient != null) {
            appRtcClient.sendLocalIceCandidateRemovals(candidates);
        }
    }

    @Override
    public void onIceConnected() {
        Log.d(TAG, "onIceConnected()");
        iceConnected = true;
    }

    @Override
    public void onIceDisconnected() {
        Log.d(TAG, "onIceDisconnected()");
        iceConnected = false;
        disconnect();
    }

    @Override
    public void onPeerConnectionClosed() {
        Log.d(TAG, "onPeerConnectionClosed()");
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        Log.d(TAG, "onPeerConnectionStatsReady()");
        // TODO : onPeerConnectionStatsReady
    }

    @Override
    public void onPeerConnectionError(final String description) {
        Log.d(TAG, "onPeerConnectionError(" + description + ")");
        // TODO : onPeerConnectionError
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }
}
