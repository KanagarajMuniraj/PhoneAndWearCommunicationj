package kanagaraj.wearcommunication.test.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import kanagaraj.wearcommunication.R;
import kanagaraj.wearcommunication.listeners.MessageAPIListener;

public class TestCommunicationActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText metSendText;
    private TextView mtvMsgDetails;
    private static final String PATH = "/Kanagu/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_communication);

        metSendText = (EditText) findViewById(R.id.activityTestCommunicationetSendText);
        mtvMsgDetails = (TextView) findViewById(R.id.activityTestCommunicationtvDetails);

        MessageAPIListener.getInstance().addListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MessageAPIListener.getInstance().removeListener(this);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.activityTestCommunicationbtnSendMsg) {

        }
    }
}
