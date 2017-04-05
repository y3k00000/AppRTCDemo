package y3k.rtc.test;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

public class MainActivity2 extends AppCompatActivity {

    Y3kAppRTCClient2 y3kAppRTCClient2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        (findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity2.this.y3kAppRTCClient2 == null) {
                    y3kAppRTCClient2 = new Y3kAppRTCClient2(MainActivity2.this, ((EditText) findViewById(R.id.editText)).getText().toString());
                    findViewById(R.id.editText).setVisibility(View.INVISIBLE);
                } else {
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
                        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                            alertDialog.dismiss();
                            MainActivity2.this.y3kAppRTCClient2.openSendFileAnnouncement(files[position]);
                        }
                    });
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
