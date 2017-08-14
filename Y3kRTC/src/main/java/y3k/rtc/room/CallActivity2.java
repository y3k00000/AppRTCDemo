/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package y3k.rtc.room;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.appspot.apprtc.AppRTCAudioManager;
import org.appspot.apprtc.AppRTCAudioManager.AudioDevice;
import org.appspot.apprtc.AppRTCAudioManager.AudioManagerEvents;
import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.AppRTCClient.RoomConnectionParameters;
import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.CallFragment;
import org.appspot.apprtc.HudFragment;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.PeerConnectionClient.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.appspot.apprtc.PercentFrameLayout;
import org.appspot.apprtc.UnhandledExceptionHandler;
import org.appspot.apprtc.WebSocketRTCClient;
import org.appspot.apprtc.Y3kAppRtcRoomParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoRenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import y3k.rtc.room.announcement.DataChannelAcknowledgement;
import y3k.rtc.room.announcement.DataChannelAnnouncement;
import y3k.rtc.room.channeldescription.FileChannelDescription;
import y3k.rtc.room.channeldescription.FileStreamChannelDescription;
import y3k.rtc.room.channelreader.ByteArrayChannelReader;
import y3k.rtc.room.channelreader.ChannelReader;
import y3k.rtc.room.channelreader.FileChannelReader;
import y3k.rtc.room.channelreader.FileStreamChannelReader;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallActivity2 extends Activity implements AppRTCClient.SignalingEvents,
        PeerConnectionClient.PeerConnectionEvents,
        CallFragment.OnCallEvents,Y3kAppRtcRoom.CallBack,DataChannelAnnouncement.Callback {

    public static final String INTENT_EXTRA_ROOMID = "extra_room_id";
    public static final String INTENT_EXTRA_Y3K_PARAMS = "extra_y3k_params";

    private static final String TAG = "CallRTCClient";
    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private PeerConnectionClient peerConnectionClient = null;
    private AppRTCClient appRtcClient;
    private SignalingParameters signalingParameters;
    private AppRTCAudioManager audioManager = null;
    private EglBase rootEglBase;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRenderScreen;
    private VideoFileRenderer videoFileRenderer;
    private final List<VideoRenderer.Callbacks> remoteRenderers =
            new ArrayList<>();
    private PercentFrameLayout localRenderLayout;
    private PercentFrameLayout remoteRenderLayout;
    private ScalingType scalingType;
    private Toast logToast;
    private boolean commandLineRun;
    private int runTimeMs;
    private boolean activityRunning;
    private RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;
    private boolean isError;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs = 0;
    private boolean micEnabled = true;
    private boolean screencaptureEnabled = false;
    private static Intent mediaProjectionPermissionResultData;
    private static int mediaProjectionPermissionResultCode;

    // Controls
    private CallFragment callFragment;
    private HudFragment hudFragment;

    Y3kAppRtcRoomParams y3kAppRtcRoomParams;
    private final ArrayList<DataChannelAnnouncement> sentAnnouncements = new ArrayList<>();
    private final ArrayList<DataChannelAnnouncement> receivedAnnouncements = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(org.appspot.apprtc.R.layout.activity_call);

        iceConnected = false;
        signalingParameters = null;
        scalingType = ScalingType.SCALE_ASPECT_FILL;

        // Create UI controls.
        localRender = (SurfaceViewRenderer) findViewById(org.appspot.apprtc.R.id.local_video_view);
        remoteRenderScreen = (SurfaceViewRenderer) findViewById(org.appspot.apprtc.R.id.remote_video_view);
        localRenderLayout = (PercentFrameLayout) findViewById(org.appspot.apprtc.R.id.local_video_layout);
        remoteRenderLayout = (PercentFrameLayout) findViewById(org.appspot.apprtc.R.id.remote_video_layout);
        callFragment = new CallFragment();
        hudFragment = new HudFragment();

        // Show/hide call control fragment on view click.
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCallControlFragmentVisibility();
            }
        };

        localRender.setOnClickListener(listener);
        remoteRenderScreen.setOnClickListener(listener);
        remoteRenderers.add(remoteRenderScreen);

        final Intent intent = getIntent();

        this.y3kAppRtcRoomParams = (Y3kAppRtcRoomParams) intent.getExtras().getSerializable(INTENT_EXTRA_Y3K_PARAMS);

        // Create video renderers.
        rootEglBase = EglBase.create();
        localRender.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderScreen.init(rootEglBase.getEglBaseContext(), null);

        localRender.setZOrderMediaOverlay(true);
        localRender.setEnableHardwareScaler(true /* enabled */);
        remoteRenderScreen.setEnableHardwareScaler(true /* enabled */);
        updateVideoView();

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        // Get Intent parameters.
        String roomId = intent.getStringExtra(INTENT_EXTRA_ROOMID);
        Log.d(TAG, "Room ID: " + roomId);
        if (roomId == null || roomId.length() == 0) {
            logAndToast(getString(org.appspot.apprtc.R.string.missing_url));
            Log.e(TAG, "Incorrect room ID in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        boolean loopback = false;
        boolean tracing = false;

        // If capturing format is not specified for screencapture, use screen resolution.
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager =
                    (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
            int videoWidth = displayMetrics.widthPixels;
            int videoHeight = displayMetrics.heightPixels;

        DataChannelParameters dataChannelParameters = new DataChannelParameters(
                y3kAppRtcRoomParams.isOrdered, //EXTRA_ORDERED
                y3kAppRtcRoomParams.maxRetransmitTimeMs, //EXTRA_MAX_RETRANSMITS_MS
                y3kAppRtcRoomParams.maxRetransmits, //EXTRA_MAX_RETRANSMITS
                y3kAppRtcRoomParams.protocol, //EXTRA_PROTOCOL
                y3kAppRtcRoomParams.isNegotiated,//EXTRA_NEGOTIATED,
                -1);

        peerConnectionParameters =
                new PeerConnectionParameters(
                        true,//EXTRA_VIDEO_CALL,
                        loopback,
                        tracing,
                        videoWidth, // videoWidth
                        videoHeight, // videoHeight
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
//        if (loopback || !DirectRTCClient.IP_PATTERN.matcher(roomId).matches()) {
            appRtcClient = new WebSocketRTCClient(y3kAppRtcRoomParams, this);
//        } else {
//            Log.i(TAG, "Using DirectRTCClient because room name looks like an IP.");
//            appRtcClient = new DirectRTCClient(this);
//        }
        // Create connection parameters.
        roomConnectionParameters = new RoomConnectionParameters(y3kAppRtcRoomParams.appRTCServer.address, roomId, false);

        // Create CPU monitor
        callFragment.setArguments(intent.getExtras());
        hudFragment.setArguments(intent.getExtras());
        // Activate call and HUD fragments and start the call.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(org.appspot.apprtc.R.id.call_fragment_container, callFragment);
        ft.add(org.appspot.apprtc.R.id.hud_fragment_container, hudFragment);
        ft.commit();

        // For command line execution run connection for <runTimeMs> and exit.
        if (commandLineRun && runTimeMs > 0) {
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            }, runTimeMs);
        }

        peerConnectionClient = new PeerConnectionClient(y3kAppRtcRoomParams);
        if (loopback) {
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            options.networkIgnoreMask = 0;
            peerConnectionClient.setPeerConnectionFactoryOptions(options);
        }
        peerConnectionClient.createPeerConnectionFactory(
                CallActivity2.this, peerConnectionParameters, CallActivity2.this);

        if (screencaptureEnabled) {
            MediaProjectionManager mediaProjectionManager =
                    (MediaProjectionManager) getApplication().getSystemService(
                            Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                    mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
        } else {
            startCall();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        mediaProjectionPermissionResultCode = resultCode;
        mediaProjectionPermissionResultData = data;
        startCall();
    }

    private boolean useCamera2() {
        return false;
    }

    private boolean captureToTexture() {
        return false;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    // Activity interfaces
    @Override
    public void onPause() {
        super.onPause();
        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null && !screencaptureEnabled) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null && !screencaptureEnabled) {
            peerConnectionClient.startVideoSource();
        }
    }

    @Override
    protected void onDestroy() {
        disconnect();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
        rootEglBase.release();
        super.onDestroy();
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
        this.scalingType = scalingType;
        updateVideoView();
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (peerConnectionClient != null) {
            peerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
    }

    @Override
    public boolean onToggleMic() {
        if (peerConnectionClient != null) {
            micEnabled = !micEnabled;
            peerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    @Override
    public void onFileSend(File file) {
        this.openSendFileAnnouncement(file);
    }

    @Override
    public void onMessageSend(String message) {
        this.sendMessageThroughProxyChannel(message);
    }

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        if (!iceConnected || !callFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
            ft.show(hudFragment);
        } else {
            ft.hide(callFragment);
            ft.hide(hudFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void updateVideoView() {
        remoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        remoteRenderScreen.setScalingType(scalingType);
        remoteRenderScreen.setMirror(false);

        if (iceConnected) {
            localRenderLayout.setPosition(
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
            localRender.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        } else {
            localRenderLayout.setPosition(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING);
            localRender.setScalingType(scalingType);
        }
        localRender.setMirror(true);

        localRender.requestLayout();
        remoteRenderScreen.requestLayout();
    }

    private void startCall() {
        if (appRtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
        logAndToast(getString(org.appspot.apprtc.R.string.connecting_to, roomConnectionParameters.roomUrl));
        appRtcClient.connectToRoom(roomConnectionParameters);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this);
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start(new AudioManagerEvents() {
            // This method will be called each time the number of available audio
            // devices has changed.
            @Override
            public void onAudioDeviceChanged(
                    AudioDevice audioDevice, Set<AudioDevice> availableAudioDevices) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            }
        });
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Update video view.
        updateVideoView();
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AudioDevice device, final Set<AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        activityRunning = false;
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (localRender != null) {
            localRender.release();
            localRender = null;
        }
        if (videoFileRenderer != null) {
            videoFileRenderer.release();
            videoFileRenderer = null;
        }
        if (remoteRenderScreen != null) {
            remoteRenderScreen.release();
            remoteRenderScreen = null;
        }
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (iceConnected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getText(org.appspot.apprtc.R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(org.appspot.apprtc.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    disconnect();
                                }
                            })
                    .create()
                    .show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer = null;
        if (screencaptureEnabled) {
            if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
                reportError("User didn't give permission to capture the screen.");
                return null;
            }
            return new ScreenCapturerAndroid(
                    mediaProjectionPermissionResultData, new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    reportError("User revoked permission to capture the screen.");
                }
            });
        } else if (useCamera2()) {
            if (!captureToTexture()) {
                reportError(getString(org.appspot.apprtc.R.string.camera2_texture_only_error));
                return null;
            }

            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        logAndToast("Creating peer connection, delay=" + delta + "ms");
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(), localRender,
                remoteRenderers, videoCapturer, signalingParameters);

        if (signalingParameters.initiator) {
            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                logAndToast("Creating ANSWER...");
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
    public void onConnectedToRoom(final SignalingParameters params) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onConnectedToRoomInternal(params);
            }
        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                    return;
                }
                logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                peerConnectionClient.setRemoteDescription(sdp);
                if (!signalingParameters.initiator) {
                    logAndToast("Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createAnswer();
                }
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                    return;
                }
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                    return;
                }
                peerConnectionClient.removeRemoteIceCandidates(candidates);
            }
        });
    }

    @Override
    public void onChannelClose() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("Remote end hung up; dropping PeerConnection");
                disconnect();
            }
        });
    }

    @Override
    public void onChannelError(final String description) {
        reportError(description);
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
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
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidate(candidate);
                }
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidateRemovals(candidates);
                }
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE connected, delay=" + delta + "ms");
                iceConnected = true;
                callConnected();
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE disconnected");
                iceConnected = false;
                disconnect();
            }
        });
    }

    @Override
    public void onPeerConnectionClosed() {
    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError && iceConnected) {
                    hudFragment.updateEncoderStatistics(reports);
                }
            }
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }

    private final ArrayList<DataChannel> currentDataChannels = new ArrayList<>();

    public ArrayList<DataChannel> getCurrentDataChannels() {
        return currentDataChannels;
    }

    @Override
    public void onDataChannel(final DataChannel dataChannel) {
        for (DataChannel currentDataChannel : new ArrayList<>(this.currentDataChannels)) {
            if (currentDataChannel.state() == DataChannel.State.CLOSED) {
                this.currentDataChannels.remove(currentDataChannel);
            }
        }
        this.currentDataChannels.add(dataChannel);
        Log.d(TAG, "onDataChannel(" + dataChannel.id() + "," + dataChannel.label() + "," + dataChannel.state() + ")");
        if (dataChannel.label().equals("ApprtcDemo data")) {
            return;
        }
        if (dataChannel.label().equals("Manage")) {
            Log.d(TAG, "got Manage Channel!!");
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
                        CallActivity2.this.onChannelAnnouncement(new DataChannelAnnouncement(CallActivity2.this, new JSONObject(receivedString)));
                    } catch (JSONException | IllegalArgumentException e) {
                        e.printStackTrace();
                        try {
                            CallActivity2.this.onChannelAcknowledgement(new DataChannelAcknowledgement(new JSONObject(receivedString)));
                        } catch (JSONException | IllegalArgumentException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
            if (this.y3kAppRtcRoomParams.isIosRemote) {
                peerConnectionClient.setManageDataChannel(dataChannel);
            }
        } else if (dataChannel.label().equals("MessageProxy")) {
            Log.d(CallActivity2.TAG, "got MessageProxy Channel!!");
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
                    CallActivity2.this.onProxyMessage(receivedString);
                }
            });
            if (this.y3kAppRtcRoomParams.isIosRemote) {
                peerConnectionClient.setMessageDataChannel(dataChannel);
            }
        } else {
            try {
                CallActivity2.this.onFileChannelConnected(dataChannel, new FileChannelDescription(new JSONObject(dataChannel.label())));
                return;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                CallActivity2.this.onFileStreamChannelConnected(dataChannel, new FileStreamChannelDescription(new JSONObject(dataChannel.label())));
                return;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            CallActivity2.this.onRawChannelConnected(dataChannel);
        }
    }

    private void onFileChannelConnected(final DataChannel dataChannel, final FileChannelDescription fileChannelDescription) {
        ChannelReader channelReader = this.onCreateFileChannelReader(dataChannel, fileChannelDescription);
        if (channelReader != null) {
            channelReader.start();
        }
    }

    private void onFileStreamChannelConnected(final DataChannel dataChannel, final FileStreamChannelDescription fileStreamChannelDescription) {
        ChannelReader channelReader = this.onCreateFileStreamChannelReader(dataChannel, fileStreamChannelDescription);
        if (channelReader != null) {
            channelReader.start();
        }
    }

    private void onRawChannelConnected(final DataChannel dataChannel) {
        ChannelReader channelReader = this.onCreateByteArrayChannelReader(dataChannel);
        if (channelReader != null) {
            channelReader.start();
        }
    }

    public void onAnnouncementAccepted(DataChannelAnnouncement announcement) {
        CallActivity2.this.receivedAnnouncements.add(announcement);
        switch (announcement.getType()) {
            case File:
            case FileStream:
                try {
                    CallActivity2.this.peerConnectionClient.getManageDataChannel().send(new DataChannel.Buffer(ByteBuffer.wrap(new DataChannelAcknowledgement(announcement, DataChannelAcknowledgement.Reply.NOTED).toJSONObject().toString().getBytes()), true));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    public void onAnnouncementDeclined(DataChannelAnnouncement announcement) {
        switch (announcement.getType()) {
            case File:
            case FileStream:
                try {
                    CallActivity2.this.peerConnectionClient.getManageDataChannel().send(new DataChannel.Buffer(ByteBuffer.wrap(new DataChannelAcknowledgement(announcement, DataChannelAcknowledgement.Reply.REFUSE).toJSONObject().toString().getBytes()), true));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    private void onChannelAcknowledgement(DataChannelAcknowledgement acknowledgement) {
        try {
            Log.d(TAG, "onChannelAcknowledgement(" + acknowledgement.toJSONObject().toString() + ")");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (DataChannelAnnouncement announcement : new ArrayList<>(this.sentAnnouncements)) {
            if (announcement.getChannelDescription().getUUID().compareTo(acknowledgement.getChannelUUID()) == 0) {
                switch (acknowledgement.getReply()) {
                    case NOTED:
//                        if(this.isIosRemote){
                        // TODO : isIosRemote
//                        } else{
                        try {
                            serveNotedDataChannelAnnouncement(announcement, CallActivity2.this.newDataChannel(announcement.getChannelDescription().toJSONObject().toString()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
//                        }
                        break;
                    case REFUSE:
                        break;
                    default:
                        break;
                }
                this.sentAnnouncements.remove(announcement);
                break;
            }
        }
    }

    private void serveNotedDataChannelAnnouncement(final DataChannelAnnouncement announcement, final DataChannel dataChannel) {
        if (announcement.getChannelDescription() instanceof FileChannelDescription) {
            final File localFile = ((FileChannelDescription) announcement.getChannelDescription()).getLocalFile();
            if (localFile != null) {
                try {
                    final FileInputStream fileInputStream = new FileInputStream(localFile);
                    new AsyncTask<Void, Integer, Void>() {
                        long totalSentByteCount = 0;

                        @Override
                        protected void onProgressUpdate(Integer... values) {
                            super.onProgressUpdate(values);
                            for (Integer progress : values) {
                                this.totalSentByteCount += progress;
                            }
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
                                    if (dataChannel.state() == DataChannel.State.CLOSING || dataChannel.state() == DataChannel.State.CLOSED) {
                                        Log.d("SendFile", "DataChannel Closed!!");
                                        return null;
                                        // TODO
                                    }
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
                            try {
                                fileInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                } catch (IllegalStateException | FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "onChannelAcknowledgement localFile==null!!");
            }
        } else if (announcement.getChannelDescription() instanceof FileStreamChannelDescription) {
            final InputStream localInputStream = ((FileStreamChannelDescription) announcement.getChannelDescription()).getFileStream();
            if (localInputStream != null) {
                try {
                    new AsyncTask<Void, Integer, Exception>() {
                        long totalSentByteCount = 0;

                        @Override
                        protected void onProgressUpdate(Integer... values) {
                            super.onProgressUpdate(values);
                            for (Integer progress : values) {
                                this.totalSentByteCount += progress;
                                FileStreamChannelDescription.SendProgressCallback sendProgressCallback = ((FileStreamChannelDescription) announcement.getChannelDescription()).getProgressCallback();
                                if (sendProgressCallback != null) {
                                    if (totalSentByteCount == 0) {
                                        sendProgressCallback.onStart();
                                    } else if (!sendProgressCallback.onSentBytes(progress)) {
                                        dataChannel.close();
                                        return;
                                    }
                                }
                            }
                        }

                        @Override
                        protected Exception doInBackground(Void... params) {
                            while (dataChannel.state() != DataChannel.State.OPEN) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            this.publishProgress(0);
                            byte[] readBuffer = new byte[51200];
                            try {
                                for (int read; (read = localInputStream.read(readBuffer)) > 0; ) {
                                    Log.d("SendFile", "sent " + read);
                                    if (dataChannel.state() == DataChannel.State.CLOSING || dataChannel.state() == DataChannel.State.CLOSED) {
                                        Log.d("SendFile", "DataChannel Closed!!");
                                        return new InterruptedIOException("Channel Closed!!");
                                        // TODO
                                    }
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
                                return e;
                            }
                            dataChannel.close();
                            try {
                                localInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Exception exception) {
                            FileStreamChannelDescription.SendProgressCallback sendProgressCallback = ((FileStreamChannelDescription) announcement.getChannelDescription()).getProgressCallback();
                            if (sendProgressCallback != null) {
                                sendProgressCallback.onFinished(exception);
                            }
                        }
                    }.execute();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "onChannelAcknowledgement localInputStream==null!!");
            }
        } else {
            // TODO : else
        }
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


    @Override
    public void onRoomStatusChanged(Y3kAppRtcRoom room, final Y3kAppRtcRoom.RoomStatus currentStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CallActivity2.this, "Room New Status : " + currentStatus.name(), Toast.LENGTH_SHORT).show();
                if (currentStatus == Y3kAppRtcRoom.RoomStatus.DISCONNECTED) {
                    CallActivity2.this.finish();
                }
            }
        });
    }

    public boolean openSendFileStreamAnnouncement(InputStream fileStream, String fileName, String filePath, long fileLength, FileStreamChannelDescription.SendProgressCallback progressCallback) {
        if (this.peerConnectionClient == null) {
            return false;
        } else {
            try {
                DataChannelAnnouncement announcement = new DataChannelAnnouncement(this, new FileStreamChannelDescription(UUID.randomUUID(), fileName, filePath, fileStream, fileLength, progressCallback));
                this.peerConnectionClient.getManageDataChannel().send(new DataChannel.Buffer(ByteBuffer.wrap(announcement.toJSONObject().toString().getBytes()), true));
                this.sentAnnouncements.add(announcement);
                return true;
            } catch (IllegalArgumentException | JSONException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public boolean openSendFileAnnouncement(File file) {
        if (this.peerConnectionClient == null) {
            return false;
        } else {
            try {
                DataChannelAnnouncement announcement = new DataChannelAnnouncement(this, new FileChannelDescription(UUID.randomUUID(), file.getName(), file, file.length()));
                this.peerConnectionClient.getManageDataChannel().send(new DataChannel.Buffer(ByteBuffer.wrap(announcement.toJSONObject().toString().getBytes()), true));
                this.sentAnnouncements.add(announcement);
                return true;
            } catch (IllegalArgumentException | JSONException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public boolean sendMessageThroughProxyChannel(String message) {
        if (this.peerConnectionClient == null) {
            return false;
        } else {
            try {
                this.peerConnectionClient.getMessageDataChannel().send(new DataChannel.Buffer(ByteBuffer.wrap(message.getBytes()), true));
                return true;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private void onChannelAnnouncement(DataChannelAnnouncement announcement) {
        try {
            Log.d(TAG, "onChannelAnnouncement(" + announcement.toJSONObject().toString() + ")");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.onDataChannelAnnouncement(announcement);
    }

    @Override
    public void onDataChannelAnnouncement(final DataChannelAnnouncement dataChannelAnnouncement) {
        switch (dataChannelAnnouncement.getType()) {
            case File:
                CallActivity2.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(CallActivity2.this)
                                .setTitle("File Receiving")
                                .setMessage("Receive file \"" + ((FileChannelDescription) dataChannelAnnouncement.getChannelDescription()).getFileName() + "\" ?")
                                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dataChannelAnnouncement.decline();
                                    }
                                })
                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dataChannelAnnouncement.accept();
                                    }
                                }).show();
                    }
                });
                break;
            case FileStream:
                CallActivity2.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(CallActivity2.this)
                                .setTitle("File Receiving")
                                .setMessage("Receive file \"" + ((FileStreamChannelDescription) dataChannelAnnouncement.getChannelDescription()).getFileName() + "\" ?")
                                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dataChannelAnnouncement.decline();
                                    }
                                })
                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dataChannelAnnouncement.accept();
                                    }
                                }).show();
                    }
                });
                break;
            default:
        }
    }

    @Override
    public FileChannelReader onCreateFileChannelReader(DataChannel dataChannel, final FileChannelDescription fileChannelDescription) {
        Log.d(CallActivity2.class.getName(), "onCreateFileChannelReader()");
        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileChannelDescription.getFileName());
        final ProgressDialog[] progressDialog = new ProgressDialog[1];
        CallActivity2.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog[0] = ProgressDialog.show(CallActivity2.this, "Receiving", "0 out of " + fileChannelDescription.getFileLength() + " bytes received...", true, false);
            }
        });
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            file.setWritable(true);
            return (FileChannelReader) new FileChannelReader(dataChannel, file).withCallback(new ChannelReader.Callback<File>() {
                long totalRead = 0;

                @Override
                public boolean onReadBytes(long count) {
                    totalRead += count;
                    CallActivity2.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog[0] != null) {
                                progressDialog[0].setMessage(totalRead + " out of " + fileChannelDescription.getFileLength() + " bytes received...");
                            }
                        }
                    });
                    return true;
                }

                @Override
                public void onFinished(final Exception e, final File result) {
                    Log.d("DataChannel.File", result.getName());
                    CallActivity2.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog[0] != null) {
                                progressDialog[0].dismiss();
                            }
                            if (e != null) {
                                Toast.makeText(CallActivity2.this, "File receive error!!\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                            } else {
                                new AlertDialog.Builder(CallActivity2.this)
                                        .setTitle("File Received")
                                        .setMessage("You'd like to open " + result.getName() + " ?")
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                                                openFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                if (result.getName().contains(".") && result.getName().lastIndexOf(".") != result.getName().length()) {
                                                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().lastIndexOf(".") + 1));
                                                    Log.d(CallActivity2.class.getName(), "mimeType = " + mimeType);
                                                    openFileIntent.setDataAndType(Uri.fromFile(result), mimeType);
                                                } else {
                                                    openFileIntent.setData(Uri.fromFile(result));
                                                }
                                                try {
                                                    CallActivity2.this.startActivity(openFileIntent);
                                                } catch (Exception e1) {
                                                    e1.printStackTrace();
                                                    Toast.makeText(CallActivity2.this, e1.getMessage(), Toast.LENGTH_LONG).show();
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
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public FileStreamChannelReader onCreateFileStreamChannelReader(DataChannel channel, final FileStreamChannelDescription channelDescription) {
        Log.d(CallActivity2.class.getName(), "onCreateFileStreamChannelReader()");
        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), channelDescription.getFileName());
        final ProgressDialog[] progressDialog = new ProgressDialog[1];
        CallActivity2.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog[0] = ProgressDialog.show(CallActivity2.this, "Receiving", "0 out of " + channelDescription.getFileLength() + " bytes received...", true, false);
            }
        });
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            file.setWritable(true);
            return (FileStreamChannelReader) new FileStreamChannelReader(channel, new FileOutputStream(file)).withCallback(new ChannelReader.Callback<OutputStream>() {
                long totalRead = 0;

                @Override
                public boolean onReadBytes(long count) {
                    totalRead += count;
                    CallActivity2.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog[0] != null) {
                                progressDialog[0].setMessage(totalRead + " out of " + channelDescription.getFileLength() + " bytes received...");
                            }
                        }
                    });
                    return true;
                }

                @Override
                public void onFinished(final Exception e, final OutputStream result) {
                    Log.d("DataChannel.File", file.getName());
                    CallActivity2.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog[0] != null) {
                                progressDialog[0].dismiss();
                            }
                            if (e != null) {
                                Toast.makeText(CallActivity2.this, "File receive error!!\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                            } else {
                                new AlertDialog.Builder(CallActivity2.this)
                                        .setTitle("File Received")
                                        .setMessage("You'd like to open " + file.getName() + " ?")
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                                                openFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                if (file.getName().contains(".") && file.getName().lastIndexOf(".") != file.getName().length()) {
                                                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(file.getName().lastIndexOf(".") + 1));
                                                    Log.d(CallActivity2.class.getName(), "mimeType = " + mimeType);
                                                    openFileIntent.setDataAndType(Uri.fromFile(file), mimeType);
                                                } else {
                                                    openFileIntent.setData(Uri.fromFile(file));
                                                }
                                                try {
                                                    CallActivity2.this.startActivity(openFileIntent);
                                                } catch (Exception e1) {
                                                    e1.printStackTrace();
                                                    Toast.makeText(CallActivity2.this, e1.getMessage(), Toast.LENGTH_LONG).show();
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
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ByteArrayChannelReader onCreateByteArrayChannelReader(DataChannel channel) {
        return null;
    }

    @Override
    public void onProxyMessage(final String message) {
        Log.d(CallActivity2.class.getName(), "onProxyMessage(" + message + ")");
        CallActivity2.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CallActivity2.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
