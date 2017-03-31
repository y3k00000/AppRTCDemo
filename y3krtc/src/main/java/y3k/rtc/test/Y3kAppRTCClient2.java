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
import android.os.AsyncTask;
import android.util.Log;

import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.AppRTCClient.RoomConnectionParameters;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.CallFragment;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.PeerConnectionClient.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.WebSocketRTCClient;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoRenderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class Y3kAppRTCClient2 implements AppRTCClient.SignalingEvents,
        PeerConnectionClient.PeerConnectionEvents,
        CallFragment.OnCallEvents {

    private static final String TAG = Y3kAppRTCClient2.class.getName();

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;
    private final List<VideoRenderer.Callbacks> remoteRenderers =
            new ArrayList<>();
    DataChannel dataChannel;
    AsyncTask currentSendingTask;
    private PeerConnectionClient peerConnectionClient = null;
    private AppRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;

    public Y3kAppRTCClient2(Activity activity, String roomId) {
        iceConnected = false;

        // Create video renderers.

        // Get Intent parameters.
        boolean loopback = false;
        boolean tracing = false;

        DataChannelParameters dataChannelParameters = new DataChannelParameters(
                true, //EXTRA_ORDERED
                -1, //EXTRA_MAX_RETRANSMITS_MS
                -1, //EXTRA_MAX_RETRANSMITS
                "", //EXTRA_PROTOCOL
                false,//EXTRA_NEGOTIATED,
                -1);

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
        roomConnectionParameters = new RoomConnectionParameters("https://appr.tc", roomId, false);

        peerConnectionClient = PeerConnectionClient.getInstance();
        if (loopback) {
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            options.networkIgnoreMask = 0;
            peerConnectionClient.setPeerConnectionFactoryOptions(options);
        }
        peerConnectionClient.createPeerConnectionFactory(
                activity, peerConnectionParameters, Y3kAppRTCClient2.this);

        appRtcClient.connectToRoom(roomConnectionParameters);
    }

    protected void onDestroy() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {

    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {

    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {

    }

    @Override
    public boolean onToggleMic() {
        return false;
    }

    @Override
    public void onCallHangUp() {
        disconnect();
    }

    private void disconnect() {
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
    }

    @Override
    public void onConnectedToRoom(final SignalingParameters params) {
        Log.d(TAG, "onConnectedToRoom()");
        signalingParameters = params;
        peerConnectionClient.createPeerConnection(null, null,
                remoteRenderers, null, signalingParameters);

        if (signalingParameters.initiator) {
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        Log.d(TAG, "onRemoteDescription(" + sdp.type + "," + sdp.description + ")");
        if (peerConnectionClient == null) {
            Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
            return;
        }
        peerConnectionClient.setRemoteDescription(sdp);
        if (!signalingParameters.initiator) {
            // Create answer. Answer SDP will be sent to offering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createAnswer();
        }
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        Log.d(TAG, "onRemoteIceCandidate(" + candidate.toString() + ")");
        if (peerConnectionClient == null) {
            Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
            return;
        }
        peerConnectionClient.addRemoteIceCandidate(candidate);
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
        Log.d(TAG, "onRemoteIceCandidatesRemoved(" + Arrays.toString(candidates) + ")");
        if (peerConnectionClient == null) {
            Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
            return;
        }
        peerConnectionClient.removeRemoteIceCandidates(candidates);
    }

    @Override
    public void onChannelClose() {
        Log.d(TAG, "onChannelClose()");
        disconnect();
    }

    @Override
    public void onChannelError(final String description) {
        Log.d(TAG, "onChannelError(" + description + ")");
    }

    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        Log.d(TAG, "onLocalDescription(" + sdp.type + "," + sdp.description + ")");
        if (appRtcClient != null) {
            if (signalingParameters.initiator) {
                appRtcClient.sendOfferSdp(sdp);
            } else {
                appRtcClient.sendAnswerSdp(sdp);
            }
        }
        if (peerConnectionParameters.videoMaxBitrate > 0) {
            Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
            peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
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
        Log.d(TAG, "onIceCandidatesRemoved(" + Arrays.toString(candidates) + ")");
        if (appRtcClient != null) {
            appRtcClient.sendLocalIceCandidateRemovals(candidates);
        }
    }

    @Override
    public void onIceConnected() {
        Log.d(TAG, "onIceConnected()");
        iceConnected = true;
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
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
        Log.d(TAG, "onPeerConnectionStatsReady(" + Arrays.toString(reports) + ");");
        if (this.dataChannel == null) {
            try {
                this.dataChannel = this.newDataChannel("Y3kChannel");
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        } else {
            if (signalingParameters.initiator && currentSendingTask == null) {
                this.currentSendingTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        byte[] stringToSendThroughChannel = "y3k is c o o l man and he doesn't give up.".getBytes();
                        for (byte b : stringToSendThroughChannel) {
                            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{b});
                            dataChannel.send(new DataChannel.Buffer(byteBuffer, true));
                        }
                        dataChannel.close();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        dataChannel = null;
                        currentSendingTask = null;
                    }
                }.execute();
            }
        }
    }

    @Override
    public void onDataChannel(final DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel("+dataChannel.id()+","+dataChannel.label()+","+dataChannel.state()+")");
        dataChannel.registerObserver(new ByteArrayChannelReader(dataChannel).withCallback(new ChannelReader.Callback<byte[]>() {
            @Override
            public void onFinished(Exception e, byte[] result) {
                if (e != null) {
                    e.printStackTrace();
                } else {
                    Log.d("ChannelReader.Callback", new String(result));
                }
            }
        }));
    }

    @Override
    public void onPeerConnectionError(final String description) {
        Log.d(TAG, "onPeerConnectionError(" + description + ")");
    }

    public DataChannel newDataChannel(String name) throws IllegalStateException {
        if (this.peerConnectionClient == null) {
            throw new IllegalStateException("peerConnectionClient==null , you fool!!");
        } else {
            DataChannel.Init dataChannelInit = new DataChannel.Init();
            dataChannelInit.ordered = peerConnectionParameters.dataChannelParameters.ordered;
            dataChannelInit.negotiated = peerConnectionParameters.dataChannelParameters.negotiated;
            dataChannelInit.maxRetransmits = peerConnectionParameters.dataChannelParameters.maxRetransmits;
            dataChannelInit.maxRetransmitTimeMs = peerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs;
            dataChannelInit.id = peerConnectionParameters.dataChannelParameters.id;
            dataChannelInit.protocol = peerConnectionParameters.dataChannelParameters.protocol;
            return peerConnectionClient.getPeerConnection().createDataChannel(name, dataChannelInit);
        }
    }
}
