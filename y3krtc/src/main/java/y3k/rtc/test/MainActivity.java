package y3k.rtc.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    Caller caller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        caller = new Caller(this).connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        caller.disconnect();
    }
}
