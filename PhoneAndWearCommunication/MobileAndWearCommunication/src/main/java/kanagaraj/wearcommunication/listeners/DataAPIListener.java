package kanagaraj.wearcommunication.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kanagaraj.wearcommunication.annotations.data.DataReceived;
import kanagaraj.wearcommunication.annotations.data.DataSendStatus;
import kanagaraj.wearcommunication.annotations.messaging.MessageReceived;
import kanagaraj.wearcommunication.annotations.messaging.MessageSendStatus;
import kanagaraj.wearcommunication.annotations.util.AnnotationConstants;

/**
 * Created by kanagaraj on 31/3/17.
 */
public class DataAPIListener extends BaseListener {

    private static DataAPIListener _me;
    private static List<Class<? extends Annotation>> annotations = new ArrayList<>();
    private static final String TAG = DataAPIListener.class.getSimpleName();
    private static final String ASSET_KEY = "assetKey";

    static {
        //Add requesting annotations before creating the object
        annotations.add(DataReceived.class);
        annotations.add(DataSendStatus.class);
    }

    public interface BitmapLoadedFromDataEventListener {
        void onBitmapLoaded(DataEvent dataEvent, Bitmap bitmap);
    }

    /**
     * Made default constructor as private for singleton purpose
     */
    private DataAPIListener() {
        super(null, null, null, null);
    }

    private DataAPIListener(Context context, String basePath) {
        super(context, TAG, basePath, annotations);
    }

    /**
     * It must be called to create the actual instance of this class
     * @param context
     * @return
     */
    public static void init(Context context, String basePath) {
        _me = new DataAPIListener(context, basePath);
    }

    /**
     * It won't create instance of this class. Just returns the reference.
     * It can be null also if init() is not called. Caller must validate this return value.
     * If it is null, caller can call init() then can retry this method
     * @return
     */
    public static DataAPIListener getInstance() {
        return _me;
    }

    /**
     * Test function, it should be called actually when there is a change in
     */
    public void onDataReceived(DataEvent dataEvent) {
        callMethodByTarget(DataReceived.class, dataEvent.getDataItem().getUri().getPath(), dataEvent);
    }

    /**
     * Sends out the message to the connected nodes
     * @param path
     * @param dataItem
     */
    public void sendData(final String path, final Bundle dataItem) {
        sendData(path, dataItem, null);
    }

    /**
     * Sends out the message to the connected nodes
     * @param path
     * @param asset
     */
    public void sendData(final String path, final Bundle bundle, final Asset asset) {

        if (isGoogleAPIClientConnected()) {

            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {

                    List<Node> connectedNodes = nodeHandler.getAvailableNodes();

                    if (connectedNodes.size() <= 0) {
                        Log.e(TAG, "Unable to send data as no node is connected.");
                        return;
                    }

                    final PutDataMapRequest dataMap = PutDataMapRequest.createWithAutoAppendedId(path);

                    if (bundle != null) {
                        dataMap.getDataMap().putAll(DataMap.fromBundle(bundle));
                    }

                    if (asset != null) {
                        dataMap.getDataMap().putAsset(ASSET_KEY, asset);
                    }

                    PutDataRequest dataRequest = dataMap.asPutDataRequest();

                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, dataRequest);
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            Log.d(TAG, "Sending DataItem isSuccess : " + dataItemResult.getStatus().isSuccess()
                                    + " Path : " + dataMap.getUri().getPath());
                            callMethodByTarget(DataSendStatus.class, null, dataItemResult);
                        }
                    });

                }
            });
        }
    }

    /**
     * Sends Resource asset
     * @param context
     * @param path
     * @param resourceId
     */
    public void sendBitmap(Context context, String path, int resourceId, Bundle bundle) {
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resourceId);
        sendBitmap(path, bundle, bmp);
    }

    /**
     * Sends File asset
     * @param path
     * @param filePath
     * @throws FileNotFoundException
     */
    public void sendBitmap(String path, String filePath, Bundle bundle) throws FileNotFoundException {
        Bitmap bmp = BitmapFactory.decodeFile(filePath);
        sendBitmap(path, bundle, bmp);
    }

    /**
     * Sends asset and data item
     * @param path
     * @param bundle
     * @param asset
     */
    public void sendAsset(final String path, Bundle bundle, byte[] asset) {
        sendData(path, bundle, Asset.createFromBytes(asset));
    }

    /**
     * Sends assets from filepath
     * @param path
     * @param filePath
     * @param bundle
     */
    public void sendAsset(String path, String filePath, Bundle bundle) {

        try {
            sendAsset(path, bundle, new FileInputStream(filePath));
        } catch (FileNotFoundException fne) {
            Log.e(TAG, "Unable to open file asset " + fne.getMessage());
        }

    }

    /**
     * Sends anytype of supplied inputstream asset
     * @param path
     * @param is
     */
    public void sendAsset(String path, Bundle bundle, InputStream is) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[1024 * 10];
        int readLen;

        try {

            while ((readLen = is.read(data)) != -1) {
                baos.write(data, 0, readLen);
            }

            sendAsset(path, bundle, baos.toByteArray());

        } catch (IOException ioe) {
            Log.e(TAG, "Unable to convert inputstream to bytearrayoutputstream.");
            ioe.printStackTrace();
        } finally {

            try {
                baos.close();
                is.close();
            } catch (IOException ioe) {}

        }

    }

    /**
     * Sends Bitmap asset
     * @param path
     * @param asset
     */
    public void sendBitmap(String path, Bundle bundle, Bitmap asset) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        asset.compress(Bitmap.CompressFormat.PNG, 100, stream);
        sendAsset(path, bundle, stream.toByteArray());
    }


    /**
     * Returns the path parameter of annotation if present
     * @param annotation
     * @return
     */
    public String getPathFromAnnotation(Annotation annotation) {

        if (annotation instanceof DataReceived) {
            return ((DataReceived) annotation).path();
        }

        return null;
    }

    /**
     * Returns calling thread from the supplied annotation
     * @param annotation
     * @return
     */
    public int getCallingThread(Annotation annotation) {

        if (annotation instanceof DataReceived) {
            return ((DataReceived) annotation).callingThread();
        }

        return AnnotationConstants.BACKGROUND_THREAD;
    }

    /**
     * Get DataMap from dataevent
     * @param dataEvent
     * @return
     */
    public static DataMap getDataMap(DataEvent dataEvent) {
        return DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
    }

    /**
     * Get Asset from dataevent
     * @param dataEvent
     * @return
     */
    public static Asset getAsset(DataEvent dataEvent) {
        return getDataMap(dataEvent).getAsset(ASSET_KEY);
    }

    /**
     * Loads Bitmap from the dataevent
     * @param dataEvent
     * @param listener
     */
    public void getBitmap(final DataEvent dataEvent, final BitmapLoadedFromDataEventListener listener) {

        if (!isGoogleAPIClientConnected()) {
            Log.e(TAG, "GoogleAPI client is not connected. Returning null");
            if (listener != null) {
                listener.onBitmapLoaded(dataEvent, null);
            }
        }

        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Asset asset = getAsset(dataEvent);

                // convert asset into a file descriptor and block until it's ready
                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        googleApiClient, asset).await().getInputStream();

                if (assetInputStream == null) {
                    Log.w(TAG, "Requested an unknown Asset.");
                    if (listener != null) {
                        listener.onBitmapLoaded(dataEvent, null);
                    }
                }

                // decode the stream into a bitmap
                if (listener != null) {
                    try {
                        final Bitmap bmp = BitmapFactory.decodeStream(assetInputStream);
                        if (Looper.getMainLooper() != Looper.myLooper()) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onBitmapLoaded(dataEvent, bmp);
                                }
                            });
                        }

                    } catch (OutOfMemoryError ooe) {
                        Log.e(TAG, "Unable to loadbitmap due to outofmemory Error");
                        ooe.printStackTrace();
                        listener.onBitmapLoaded(dataEvent, null);
                    }
                }
            }
        });

    }

    /**
     * Dumps DataItem Values
     * @param dataEvent
     */
    public static void dumpDataEvent(DataEvent dataEvent) {

        DataItem dataItem = dataEvent.getDataItem();
        DataMap dataMap = getDataMap(dataEvent);

        Log.d(TAG, "DataPath : " + dataItem.getUri().toString());

        for (String key : dataMap.keySet()) {
            Log.d(TAG, "Key : " + key + " Value : " + dataMap.get(key));
        }

    }

}
