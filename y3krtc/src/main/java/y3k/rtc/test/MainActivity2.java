package y3k.rtc.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

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
                    ((EditText) findViewById(R.id.editText)).setText("");
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
