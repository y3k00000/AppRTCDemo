package y3k.rtc.test;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.appspot.apprtc.WebSocketChannelClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    Y3kAppRTCClient y3kAppRTCClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        (findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.this.y3kAppRTCClient == null) {
                    y3kAppRTCClient = new Y3kAppRTCClient(MainActivity.this, MainActivity.this.webSocketMessageCallback).connect(((EditText) findViewById(R.id.editText)).getText().toString());
                    ((EditText) findViewById(R.id.editText)).setText("");
                } else {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, 6688);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 6688:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            y3kAppRTCClient.postMessage(Y3kAppRTCClient.Command.SENDING_START.name());
                            try {
                                Bitmap bitmap = ((Bitmap) data.getExtras().get("data"));
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                                byte[] writeBuffer = new byte[1024];
                                byteArrayOutputStream = new ByteArrayOutputStream();
                                for (int readCount; (readCount = byteArrayInputStream.read(writeBuffer)) > 0; ) {
                                    byteArrayOutputStream.write(writeBuffer, 0, readCount);
                                    Log.d("WebSocket", "write bytes " + readCount);
                                }
                                byteArrayInputStream.close();
                                y3kAppRTCClient.postMessage(Y3kAppRTCClient.encodeByteArrayToHexString(byteArrayOutputStream.toByteArray()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            y3kAppRTCClient.postMessage(Y3kAppRTCClient.Command.SENDING_END.name());
                        }
                    }).start();
            }
        }
    }

    WebSocketChannelClient.WebSocketChannelEvents webSocketMessageCallback = new WebSocketChannelClient.WebSocketChannelEvents() {
        OutputStream currentOutputStream;
        File currentWritingFile;

        @Override
        public void onWebSocketMessage(final String message) {
            Log.d("WebSocket", "onWebSocketMessage(" + message + ")");
            try {
                JSONObject jsonObject = new JSONObject(message);
                String msg = jsonObject.getString("msg");
                Log.d("WebSocket", "msg = " + msg);
                try {
                    switch (Y3kAppRTCClient.Command.valueOf(msg)) {
                        case SENDING_START:
//                        this.currentWritingFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/y3k_" + new Date().getTime() + ".jpg");
//                        try {
//                            if (this.currentWritingFile.createNewFile()) {
//                                this.currentOutputStream = new FileOutputStream(this.currentWritingFile);
//                            } else {
//                                this.currentWritingFile = null;
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            this.currentWritingFile = null;
//                        }
                            break;
                        case SENDING_END:
//                        if (this.currentOutputStream != null) {
//                            try {
//                                this.currentOutputStream.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            if (this.currentWritingFile != null) {
//                                Uri fileUri = Uri.fromFile(this.currentWritingFile);
//                                startActivity(new Intent(Intent.ACTION_VIEW).setData(fileUri));
//                            }
//                            this.currentOutputStream = null;
//                            this.currentWritingFile = null;
//                        }
                            break;
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    final byte[] bitMapData = Y3kAppRTCClient.DecodeHexStringToByteArray(msg);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ImageView imageView = new ImageView(MainActivity.this);
                            imageView.setImageBitmap(BitmapFactory.decodeByteArray(bitMapData, 0, bitMapData.length));
                            new AlertDialog.Builder(MainActivity.this)
                                    .setView(imageView)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "onWebSocketMessage(" + message + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        @Override
        public void onWebSocketClose() {
            Log.d("WebSocket", "onWebSocketClose()");
            callerExit();
        }

        @Override
        public void onWebSocketError(final String description) {
            Log.d("WebSocket", "onWebSocketError(" + description + ")");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "onWebSocketError(" + description + ")", Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onBinaryMessage(byte[] binary) {
            Log.d("WebSocket", "onWebSocketError(" + (binary == null ? "null" : binary.length) + ")");
//            if (binary != null && this.currentOutputStream != null) {
//                try {
//                    this.currentOutputStream.write(binary);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    };

    private void callerExit() {
        if (this.y3kAppRTCClient != null) {
            this.y3kAppRTCClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        callerExit();
        super.onDestroy();
    }
}
