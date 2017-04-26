package kanagaraj.wearcommunication.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;

import kanagaraj.wearcommunication.listeners.DataAPIListener;
import kanagaraj.wearcommunication.listeners.MessageAPIListener;

public class WearDataListenerService extends WearableListenerService {

    private final String TAG = WearDataListenerService.class.getSimpleName();

    public WearDataListenerService() {
    }

    @Override
    public void onMessageReceived(MessageEvent msgEvent) {

        Log.d(TAG, "Wear MessageAPI received");

        MessageAPIListener.getInstance().onMessageReceived(msgEvent);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.d(TAG, "onDataChanged called");

        final ArrayList<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        for (DataEvent event : events) {

            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataAPIListener.getInstance().onDataReceived(event);
            }

        }
    }

}
