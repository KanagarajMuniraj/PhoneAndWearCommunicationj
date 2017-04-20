package kanagaraj.connection.phoneandwear;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import java.util.Timer;
import java.util.TimerTask;

import kanagaraj.wearcommunication.annotations.messaging.MessageReceived;
import kanagaraj.wearcommunication.annotations.messaging.MessageSendStatus;
import kanagaraj.wearcommunication.annotations.util.AnnotationConstants;
import kanagaraj.wearcommunication.listeners.MessageAPIListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText metSendText;
    private EditText metPath;
    private TextView mtvMsgDetails;
    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        metPath = (EditText) findViewById(R.id.activityMainetPath);
        metSendText = (EditText) findViewById(R.id.activityMainetSendText);
        mtvMsgDetails = (TextView) findViewById(R.id.activityMaintvDetails);

        findViewById(R.id.activityMainbtnSendMsg).setOnClickListener(this);

        MessageAPIListener.init(getApplicationContext(), "/Kanagu");
        MessageAPIListener.getInstance().addListener(this);


        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                MessageAPIListener.getInstance().sendMessage("/Kanagu/SendMessage", "From Phone".getBytes());
            }
        }, 2000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MessageAPIListener.getInstance().removeListener(this);
    }

    @MessageReceived(path = "/WearKanagu/test")
    public void OnMessageReceived(MessageEvent msgEvent) {
        Toast.makeText(this, "Message Received Path : " + msgEvent.getPath() + " Data " + new String(msgEvent.getData()),
                Toast.LENGTH_LONG).show();
    }

    @MessageSendStatus
    public void onMessageSent(MessageApi.SendMessageResult sendMessageResult) {
        Toast.makeText(this, "onMessageSent AnnotationMethodCalled", Toast.LENGTH_LONG).show();
    }

    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.activityMainbtnSendMsg:
                MessageAPIListener.getInstance().sendMessage(metPath.getText().toString(), metSendText.getText().toString().getBytes());
                break;
        }

    }
}
