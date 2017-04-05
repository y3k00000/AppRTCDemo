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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.AppRTCClient.RoomConnectionParameters;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.CallFragment;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.PeerConnectionClient.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.WebSocketRTCClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoRenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import y3k.rtc.test.announcement.DataChannelAcknowledgement;
import y3k.rtc.test.announcement.DataChannelAnnouncement;
import y3k.rtc.test.channeldescription.FileChannelDescription;
import y3k.rtc.test.channelreader.ChannelReader;
import y3k.rtc.test.channelreader.FileChannelReader;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class Y3kAppRTCClient2 implements AppRTCClient.SignalingEvents,
        PeerConnectionClient.PeerConnectionEvents,
        CallFragment.OnCallEvents {

    private static final String TAG = Y3kAppRTCClient2.class.getName();

    private static final int STAT_CALLBACK_PERIOD = 1000;
    final Activity activity;
    private PeerConnectionClient peerConnectionClient = PeerConnectionClient.getInstance();
    private AppRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;

    public Y3kAppRTCClient2(Activity activity, String roomId) {
        this.activity = activity;
        iceConnected = false;

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
                        false, //EXTRA_HWCODEC_ENABLED
                        false, //EXTRA_FLEXFEC_ENABLED
                        0, //EXTRA_AUDIO_BITRATE
                        "OPUS", //EXTRA_AUDIOCODEC
                        true, //EXTRA_NOAUDIOPROCESSING_ENABLED
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
                new ArrayList<VideoRenderer.Callbacks>(), null, signalingParameters);

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
//        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
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
    }

    @Override
    public void onDataChannel(final DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel(" + dataChannel.id() + "," + dataChannel.label() + "," + dataChannel.state() + ")");
//        if (dataChannel.label().equals("ApprtcDemo data")) {
//            // ApprtcDamo default DataChannel -> skip
//            return;
//        }
        if (dataChannel.label().equals("Manage")) {
            Log.d(Y3kAppRTCClient2.TAG, "got Manage Channel!!");
            dataChannel.registerObserver(new DataChannel.Observer() {
                @Override
                public void onBufferedAmountChange(long l) {

                }

                @Override
                public void onStateChange() {
                    Log.d(TAG, "Manage Channel onStateChanged(" + dataChannel.state().name() + ")");
                }

                @Override
                public void onMessage(DataChannel.Buffer buffer) {
                    Log.d(TAG, "Manage Channel onMessage!!");
                    byte[] receivedBytes = new byte[buffer.data.remaining()];
                    buffer.data.get(receivedBytes);
                    String receivedString = new String(receivedBytes);
                    try {
                        Y3kAppRTCClient2.this.onChannelAnnouncement(new DataChannelAnnouncement(new JSONObject(receivedString)));
                    } catch (JSONException | IllegalArgumentException e) {
                        e.printStackTrace();
                        try {
                            Y3kAppRTCClient2.this.onChannelAcknowledgement(new DataChannelAcknowledgement(new JSONObject(receivedString)));
                        } catch (JSONException | IllegalArgumentException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
            this.manageDataChannel = dataChannel;
        } else {
            try {
                FileChannelDescription fileChannelDescription = new FileChannelDescription(new JSONObject(dataChannel.label()));
                final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileChannelDescription.getFileName());
                try {
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                    file.setWritable(true);
                    ChannelReader fileChannelReader = new FileChannelReader(dataChannel, file).withCallback(new ChannelReader.Callback<File>() {
                        @Override
                        public void onFinished(final Exception e, final File result) {
                            Log.d("DataChannel.File", result.getName());
                            Y3kAppRTCClient2.this.activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (e != null) {
                                        Toast.makeText(Y3kAppRTCClient2.this.activity, "File receive error!!\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                                    } else {
                                        new AlertDialog.Builder(Y3kAppRTCClient2.this.activity)
                                                .setTitle("File Received")
                                                .setMessage("You'd like to open " + result.getName() + " ?")
                                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent openFileIntent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(result));
                                                        if (file.getName().lastIndexOf(".") != file.getName().length()) {
                                                            openFileIntent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().lastIndexOf(".") + 1)));
                                                        }
                                                        try {
                                                            Y3kAppRTCClient2.this.activity.startActivity(openFileIntent);
                                                        } catch (Exception e1) {
                                                            e1.printStackTrace();
                                                            Toast.makeText(Y3kAppRTCClient2.this.activity, e1.getMessage(), Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                })
                                                .setNegativeButton("No", null)
                                                .show();
                                    }
                                }
                            });
                        }
                    });
                    fileChannelReader.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    DataChannel manageDataChannel;

    private void onChannelAnnouncement(final DataChannelAnnouncement announcement) {
        try {
            Log.d(TAG, "onChannelAnnouncement(" + announcement.toJSONObject().toString() + ")");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        switch (announcement.getType()) {
            case File:
                this.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(Y3kAppRTCClient2.this.activity)
                                .setTitle("File Receiving")
                                .setMessage("Receive file \"" + ((FileChannelDescription) announcement.getChannelDescription()).getFileName() + "\" ?")
                                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            Y3kAppRTCClient2.this.peerConnectionClient.getManageDataChannel().send(new DataChannel.Buffer(ByteBuffer.wrap(new DataChannelAcknowledgement(announcement, DataChannelAcknowledgement.Reply.REFUSE).toJSONObject().toString().getBytes()), true));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            Y3kAppRTCClient2.this.peerConnectionClient.getManageDataChannel().send(new DataChannel.Buffer(ByteBuffer.wrap(new DataChannelAcknowledgement(announcement, DataChannelAcknowledgement.Reply.NOTED).toJSONObject().toString().getBytes()), true));
                                            Y3kAppRTCClient2.this.receivedAnnouncements.add(announcement);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).show();
                    }
                });
                break;
            default:
        }
    }

    private void onChannelAcknowledgement(final DataChannelAcknowledgement acknowledgement) {
        try {
            Log.d(TAG, "onChannelAcknowledgement(" + acknowledgement.toJSONObject().toString() + ")");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        switch (acknowledgement.getReply()) {
            case NOTED:
                for (final DataChannelAnnouncement announcement : new ArrayList<>(this.sentAnnouncements)) {
                    if (announcement.getChannelDescription().getUUID().compareTo(acknowledgement.getChannelUUID()) == 0) {
                        if (announcement.getChannelDescription() instanceof FileChannelDescription) {
                            final File localFile = ((FileChannelDescription) announcement.getChannelDescription()).getLocalFile();
                            this.activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final FileInputStream fileInputStream = new FileInputStream(localFile);
                                        final DataChannel dataChannel = Y3kAppRTCClient2.this.newDataChannel(announcement.getChannelDescription().toJSONObject().toString());
                                        new AsyncTask<Void, Integer, Void>() {
                                            ProgressDialog sendingProgressDialog;
                                            long totalSentByteCount = 0;

                                            @Override
                                            protected void onPreExecute() {
                                                this.sendingProgressDialog = ProgressDialog.show(Y3kAppRTCClient2.this.activity, "Sending", this.totalSentByteCount + "/" + localFile.length() + " bytes.");
                                                super.onPreExecute();
                                            }

                                            @Override
                                            protected void onProgressUpdate(Integer... values) {
                                                super.onProgressUpdate(values);
                                                for (Integer progress : values) {
                                                    this.totalSentByteCount += progress;
                                                }
                                                this.sendingProgressDialog.setMessage(totalSentByteCount + "/" + localFile.length() + " bytes.");
                                            }

                                            @Override
                                            protected Void doInBackground(Void... params) {
                                                while (dataChannel.state() != DataChannel.State.OPEN) {
                                                    try {
                                                        Thread.sleep(1000);
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                byte[] readBuffer = new byte[51200];
                                                try {
                                                    for (int read; (read = fileInputStream.read(readBuffer)) > 0; ) {
                                                        Log.d("SendFile", "sent " + read);
                                                        ByteBuffer byteBuffer = ByteBuffer.wrap(Arrays.copyOf(readBuffer, read));
                                                        dataChannel.send(new DataChannel.Buffer(byteBuffer, true));
                                                        this.publishProgress(read);
                                                        while (dataChannel.bufferedAmount() > 0) {
                                                            Log.d("SendFile", "dataChannel.bufferedAmount = " + dataChannel.bufferedAmount());
                                                            try {
                                                                Thread.sleep(50);
                                                            } catch (InterruptedException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                dataChannel.close();
                                                return null;
                                            }

                                            @Override
                                            protected void onPostExecute(Void aVoid) {
                                                super.onPostExecute(aVoid);
                                                this.sendingProgressDialog.dismiss();
                                            }
                                        }.execute();
                                    } catch (IllegalStateException | FileNotFoundException | JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } else {
                            // TODO : else
                        }
                        this.sentAnnouncements.remove(announcement);
                        return;
                    }
                }
                break;
            case REFUSE:
                for (DataChannelAnnouncement announcement : new ArrayList<>(this.sentAnnouncements)) {
                    if (announcement.getChannelDescription().getUUID().compareTo(acknowledgement.getChannelUUID()) == 0) {
                        this.sentAnnouncements.remove(announcement);
                    }
                }
                break;
            default:
                break;
        }
    }

    private final ArrayList<DataChannelAnnouncement> sentAnnouncements = new ArrayList<>();
    private final ArrayList<DataChannelAnnouncement> receivedAnnouncements = new ArrayList<>();

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

    public void openSendFileAnnouncement(File file) {
        try {
            DataChannelAnnouncement announcement = new DataChannelAnnouncement(new FileChannelDescription(UUID.randomUUID(), file.getName(), file, file.length()));
            this.peerConnectionClient.getManageDataChannel().send(new DataChannel.Buffer(ByteBuffer.wrap(announcement.toJSONObject().toString().getBytes()), true));
            this.sentAnnouncements.add(announcement);
        } catch (IllegalArgumentException | JSONException e) {
            e.printStackTrace();
        }
    }
}
