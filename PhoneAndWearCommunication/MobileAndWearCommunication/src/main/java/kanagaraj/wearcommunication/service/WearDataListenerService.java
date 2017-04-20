package kanagaraj.wearcommunication.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import kanagaraj.wearcommunication.listeners.MessageAPIListener;

public class WearDataListenerService extends WearableListenerService {

    public WearDataListenerService() {
    }

    @Override
    public void onMessageReceived(MessageEvent msgEvent) {

        Log.d("MessageAPIListener", "Wear MessageAPI received");

        MessageAPIListener.getInstance().onMessageReceived(msgEvent);
    }

}
