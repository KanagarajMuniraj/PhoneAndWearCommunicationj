package kanagaraj.connection.phoneandwear;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import java.util.Timer;
import java.util.TimerTask;

import kanagaraj.connection.phoneandwear.util.Constants;
import kanagaraj.wearcommunication.annotations.data.DataReceived;
import kanagaraj.wearcommunication.annotations.data.DataSendStatus;
import kanagaraj.wearcommunication.annotations.messaging.MessageReceived;
import kanagaraj.wearcommunication.annotations.messaging.MessageSendStatus;
import kanagaraj.wearcommunication.annotations.util.AnnotationConstants;
import kanagaraj.wearcommunication.listeners.DataAPIListener;
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

        MessageAPIListener.init(getApplicationContext(), Constants.PHONE_PATH_PREFIX);
        MessageAPIListener.getInstance().addListener(this);

        DataAPIListener.init(getApplicationContext(), Constants.PHONE_PATH_PREFIX);
        DataAPIListener.getInstance().addListener(this);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                MessageAPIListener.getInstance().sendMessage("/Kanagu/SendMessage", "From Phone".getBytes());
                Bundle bundle = new Bundle();
                bundle.putInt("phoneData", 100);
                DataAPIListener.getInstance().sendData(Constants.PHONE_PATH_PREFIX + "/Data/SendData", bundle);
            }
        }, 2000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MessageAPIListener.getInstance().removeListener(this);
    }

    @MessageReceived(path = Constants.WEAR_PATH_PREFIX + "/test")
    public void OnMessageReceived(MessageEvent msgEvent) {
        Toast.makeText(this, "Message Received Path : " + msgEvent.getPath() + " Data " + new String(msgEvent.getData()),
                Toast.LENGTH_LONG).show();
    }

    @MessageSendStatus
    public void onMessageSent(MessageApi.SendMessageResult sendMessageResult) {
        Toast.makeText(this, "onMessageSent AnnotationMethodCalled", Toast.LENGTH_LONG).show();
    }

    @DataReceived(path = Constants.WEAR_PATH_PREFIX + "/Data/SendData/", callingThread = AnnotationConstants.MAIN_THREAD)
    public void onDataReceived(DataEvent dataEvent) {
        Toast.makeText(this, "DataItemReceived", Toast.LENGTH_LONG).show();
        DataAPIListener.dumpDataEvent(dataEvent);
    }

    @DataReceived(path = Constants.WEAR_PATH_PREFIX + "/Data/SendImg")
    public void onImgReceived(DataEvent dataEvent) {
        DataAPIListener.getInstance().getBitmap(dataEvent, new DataAPIListener.BitmapLoadedFromDataEventListener() {
            @Override
            public void onBitmapLoaded(DataEvent dataEvent, Bitmap bitmap) {
                ((ImageView) findViewById(R.id.activityMainivImg)).setImageBitmap(bitmap);
            }
        });
    }

    @DataSendStatus
    public void onDataSent(DataApi.DataItemResult dataItemResult) {
        Toast.makeText(this, "DataSendSeuccessfully", Toast.LENGTH_LONG).show();
    }

    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.activityMainbtnSendMsg:
                MessageAPIListener.getInstance().sendMessage(metPath.getText().toString(), metSendText.getText().toString().getBytes());
                break;
        }

    }
}
