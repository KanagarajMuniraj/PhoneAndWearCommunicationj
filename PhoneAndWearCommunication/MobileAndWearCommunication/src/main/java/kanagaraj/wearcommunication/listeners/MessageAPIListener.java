package kanagaraj.wearcommunication.listeners;

import android.content.Context;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import kanagaraj.wearcommunication.annotations.messaging.MessageReceived;
import kanagaraj.wearcommunication.annotations.messaging.MessageSendStatus;
import kanagaraj.wearcommunication.annotations.util.AnnotationConstants;

/**
 * Created by kanagaraj on 31/3/17.
 */
public class MessageAPIListener extends BaseListener {

    private static MessageAPIListener   _me;
    private static List<Class<? extends Annotation>> annotations = new ArrayList<>();
    private static final String TAG = MessageAPIListener.class.getSimpleName();

    static {
        //Add requesting annotations before creating the object
        annotations.add(MessageReceived.class);
        annotations.add(MessageSendStatus.class);
    }

    /**
     * Made default constructor as private for singleton purpose
     */
    private MessageAPIListener() {
        super(null, null, null, null);
    }

    private MessageAPIListener(Context context, String basePath) {
        super(context, TAG, basePath, annotations);
    }

    /**
     * It must be called to create the actual instance of this class
     * @param context
     * @return
     */
    public static void init(Context context, String basePath) {
        _me = new MessageAPIListener(context, basePath);
    }

    /**
     * It won't create instance of this class. Just returns the reference.
     * It can be null also if init() is not called. Caller must validate this return value.
     * If it is null, caller can call init() then can retry this method
     * @return
     */
    public static MessageAPIListener getInstance() {
        return _me;
    }

    /**
     * Test function, it should be called actually when there is a change in
     */
    public void onMessageReceived(MessageEvent msgEvent) {
        callMethodByTarget(MessageReceived.class, msgEvent.getPath(), msgEvent);
    }

    /**
     * Sends out the message to the connected nodes
     * @param path
     * @param data
     */
    public void sendMessage(final String path, final byte[] data) {

        if (isGoogleAPIClientConnected()) {

            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {

                    //Add uniquenes to the each message. Incase of same action or data on second time
                    //It won't be delivered. So making the unique all the time.
                    final String uniquePath = path + "/" + System.currentTimeMillis();

                    for (final Node node : nodeHandler.getAvailableNodes()) {

                        Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, data)
                                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                    @Override
                                    public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                        Log.d(TAG, "Sending Message path " + uniquePath + " NodeId send success? "
                                                + sendMessageResult.getStatus().isSuccess());
                                        callMethodByTarget(MessageSendStatus.class, null, sendMessageResult);
                                    }
                                });
                    }

                }
            });
        }
    }

    /**
     * Returns the path parameter of annotation if present
     * @param annotation
     * @return
     */
    public String getPathFromAnnotation(Annotation annotation) {

        if (annotation instanceof MessageReceived) {
            return ((MessageReceived) annotation).path();
        }

        return null;
    }

    /**
     * Returns calling thread from the supplied annotation
     * @param annotation
     * @return
     */
    public int getCallingThread(Annotation annotation) {

        if (annotation instanceof MessageReceived) {
            return ((MessageReceived) annotation).callingThread();
        }

        return AnnotationConstants.BACKGROUND_THREAD;
    }

}
