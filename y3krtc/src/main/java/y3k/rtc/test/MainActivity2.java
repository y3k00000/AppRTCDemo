package y3k.rtc.test;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.webrtc.DataChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity2 extends AppCompatActivity {

    Y3kAppRTCClient2 y3kAppRTCClient2;
    DataChannel dataChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        (findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity2.this.y3kAppRTCClient2 == null) {
                    y3kAppRTCClient2 = new Y3kAppRTCClient2(MainActivity2.this, ((EditText) findViewById(R.id.editText)).getText().toString());
                    ((EditText) findViewById(R.id.editText)).setText("");
                } else {
                    if (dataChannel == null) {
                        final File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
                        ListView listView = new ListView(MainActivity2.this);
                        listView.setAdapter(new BaseAdapter() {
                            @Override
                            public int getCount() {
                                return files.length;
                            }

                            @Override
                            public Object getItem(int position) {
                                return files[position];
                            }

                            @Override
                            public long getItemId(int position) {
                                return 0;
                            }

                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                TextView textView = new TextView(MainActivity2.this);
                                textView.setText(files[position].getName());
                                textView.setTag(files[position]);
                                return textView;
                            }
                        });
                        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity2.this)
                                .setView(listView)
                                .show();
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                alertDialog.dismiss();
                                try {
                                    final FileInputStream fileInputStream = new FileInputStream(files[position]);
                                    dataChannel = MainActivity2.this.y3kAppRTCClient2.newDataChannel(files[position].getName());
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            while(dataChannel.state()!= DataChannel.State.OPEN) {
                                                try {
                                                    Thread.sleep(1000);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            byte[] readBuffer = new byte[51200];
                                            try {
                                                for(int read;(read=fileInputStream.read(readBuffer))>0;){
                                                    Log.d("SendFile", "sent "+read);
                                                    ByteBuffer byteBuffer = ByteBuffer.wrap(Arrays.copyOf(readBuffer,read));
                                                    dataChannel.send(new DataChannel.Buffer(byteBuffer, true));
                                                    while (dataChannel.bufferedAmount()>0){
                                                        Log.d("SendFile", "dataChannel.bufferedAmount = "+dataChannel.bufferedAmount());
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
                                            dataChannel = null;
                                        }
                                    }.execute();
                                } catch (IllegalStateException | FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        y3kAppRTCClient2.onDestroy();
        super.onDestroy();
    }
}
