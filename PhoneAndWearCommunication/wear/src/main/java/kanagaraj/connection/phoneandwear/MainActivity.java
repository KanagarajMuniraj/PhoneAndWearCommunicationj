package kanagaraj.connection.phoneandwear;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.view.WatchViewStub;
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

public class MainActivity extends Activity implements View.OnClickListener {

    private EditText metSendText;
    private EditText metPath;
    private TextView mtvMsgDetails;
    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                metPath = (EditText) stub.findViewById(R.id.activityMainetPath);
                metSendText = (EditText) stub.findViewById(R.id.activityMainetSendText);
                mtvMsgDetails = (TextView) stub.findViewById(R.id.activityMaintvDetails);
                stub.findViewById(R.id.activityMainbtnSendMsg).setOnClickListener(MainActivity.this);
            }
        });

        MessageAPIListener.init(getApplicationContext(), "/WearKanagu");
        MessageAPIListener.getInstance().addListener(this);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                MessageAPIListener.getInstance().sendMessage("/WearKanagu/test", null);
            }
        }, 2000);
    }

    @MessageReceived(path="/Kanagu/SendMessage", callingThread = AnnotationConstants.MAIN_THREAD)
    public void OnMessageTestReceived(MessageEvent msgEvent) {
        Toast.makeText(this, "OnMessageTestReceived method called", Toast.LENGTH_LONG).show();
    }

    @MessageReceived()
    public void onMessageReceived(MessageEvent msgEvent) {
        Toast.makeText(this, "onMessageReceived method called", Toast.LENGTH_LONG).show();
    }

    @MessageSendStatus
    public void onMessageSent(MessageApi.SendMessageResult sendMessageResult) {
        Toast.makeText(this, "onMessageSent AnnotationMethodCalled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MessageAPIListener.getInstance().removeListener(this);
    }

    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.activityMainbtnSendMsg:
                MessageAPIListener.getInstance().sendMessage(metPath.getText().toString(), metSendText.getText().toString().getBytes());
                break;
        }

    }
}
