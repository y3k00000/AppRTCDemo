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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

@Deprecated
public class MainActivity extends AppCompatActivity {

    Y3kAppRTCClient y3kAppRTCClient;
    WebSocketChannelClient.WebSocketChannelEvents webSocketMessageCallback = new WebSocketChannelClient.WebSocketChannelEvents() {
        OutputStream currentOutputStream;

        @Override
        public void onWebSocketMessage(final String message) {
            Log.d("WebSocket", "onWebSocketMessage(" + message + ")");
            try {
                JSONObject jsonObject = new JSONObject(message);
                String messageInJSON = jsonObject.getString("msg");
                Log.d("WebSocket", "msg = " + messageInJSON);
                Log.d("WebSocket", "msg.length = " + messageInJSON.length());
                try {
                    switch (Y3kAppRTCClient.Command.valueOf(messageInJSON)) {
                        case SENDING_START:
                            this.currentOutputStream = new ByteArrayOutputStream();
                            break;
                        case SENDING_END:
                            if (this.currentOutputStream != null) {
                                if(this.currentOutputStream instanceof ByteArrayOutputStream){
                                    Log.d("WebSocket", "ByteArrayOutputStream.size() = "+((ByteArrayOutputStream) this.currentOutputStream).size());
                                    final Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(((ByteArrayOutputStream) this.currentOutputStream).toByteArray()));
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ImageView imageView = new ImageView(MainActivity.this);
                                            imageView.setImageBitmap(bitmap);
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setView(imageView)
                                                    .setPositiveButton("OK", null)
                                                    .show();
                                        }
                                    });
                                }
                                try {
                                    this.currentOutputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                this.currentOutputStream = null;
                            }
                            break;
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    if(this.currentOutputStream!=null){
                        try {
                            this.currentOutputStream.write(Y3kAppRTCClient.DecodeHexStringToByteArray(messageInJSON));
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
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
        }
    };

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
                                byte[] readBuffer = new byte[102400];
                                for (int readCount; (readCount = byteArrayInputStream.read(readBuffer)) > 0; ) {
                                    y3kAppRTCClient.postMessage(Y3kAppRTCClient.encodeByteArrayToHexString(Arrays.copyOf(readBuffer,readCount)));
                                }
                                byteArrayInputStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            y3kAppRTCClient.postMessage(Y3kAppRTCClient.Command.SENDING_END.name());
                        }
                    }).start();
            }
        }
    }

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
