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
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.AppRTCClient.RoomConnectionParameters;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.PeerConnectionClient.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.WebSocketRTCClient;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class Caller implements AppRTCClient.SignalingEvents,
        PeerConnectionClient.PeerConnectionEvents {

    private static final String TAG = Caller.class.getName();
    private final Activity activity;
    private WebSocketRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected = false;
    private boolean isError;
    private boolean micEnabled = true;

    // Controls
    public Caller(Activity activity) {
        this.activity = activity;

        boolean loopback = false;
        boolean tracing = true;

        // TODO Room URI
        Uri roomUri = Uri.parse("https://appr.tc");

        // TODO Room ID
        String roomId = "y3kTest"+ new Date().getTime();
        Log.d(TAG, "Room ID: " + roomId);

        if (loopback) {
            roomId = Integer.toString((new Random()).nextInt(100000000));
        }

        // If capturing format is not specified for screencapture, use screen resolution.
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
        appRtcClient = new WebSocketRTCClient(this);
//    } else {
//      Log.i(TAG, "Using DirectRTCClient because room name looks like an IP.");
//      appRtcClient = new DirectRTCClient(this);
//    }
        // Create connection parameters.
        roomConnectionParameters = new RoomConnectionParameters(roomUri.toString(), roomId, loopback);
    }

    public Caller connect() {
        appRtcClient.connectToRoom(roomConnectionParameters);
        return this;
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
        new Thread(()->{
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.appRtcClient.getWsClient().getHandler().post(()-> this.appRtcClient.getWsClient().post("y3k is cool"));
        }).start();
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
}
