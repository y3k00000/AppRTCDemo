package y3k.rtc.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    Caller caller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        (findViewById(R.id.button)).setOnClickListener((v) -> {
            if (this.caller == null) {
                caller = new Caller(this).connect(((EditText) findViewById(R.id.editText)).getText().toString());
                ((EditText) findViewById(R.id.editText)).setText("");
            } else {
                caller.sendMessage(((EditText) findViewById(R.id.editText)).getText().toString());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.caller != null) {
            this.caller.disconnect();
        }
    }
}
