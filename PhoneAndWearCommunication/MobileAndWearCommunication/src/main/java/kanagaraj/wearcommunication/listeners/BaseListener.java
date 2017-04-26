package kanagaraj.wearcommunication.listeners;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kanagaraj.wearcommunication.annotations.util.AnnotationConstants;
import kanagaraj.wearcommunication.util.NodeHandler;

/**
 * Created by kanagaraj on 3/4/17.
 */
public abstract class BaseListener {

    protected ArrayList<WeakReference<Object>>                      listenerClasses;
    protected HashMap<Object, List<AnnotationProperties>>           objectAnnotationCache;
    private ArrayList<Class<? extends Annotation>>                  annotationCollection;

    protected GoogleApiClient   googleApiClient;
    private final String        TAG;
    private final int           MAX_FUNCTION_PARAMETERS_SUPPORTED = 3;
    protected NodeHandler       nodeHandler;

    protected ThreadPoolExecutor    threadPoolExecutor;
    private final int MAX_CORES = Runtime.getRuntime().availableProcessors();

    //Base path to determine fallback messages. On sending message, it will fallback to the sender
    //as well. In that case it can be avoided if base path provided.
    private final String BASE_PATH;

    protected final Handler mMainHandler;
    private final Handler mBkgndHandler;

    private class AnnotationProperties {
        Method method;
        Annotation annotation;
    }

    private GoogleApiClient.ConnectionCallbacks mConnCallback = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, "GoogleApiClient Connected");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "GoogleApiClient connection suspended");
        }
    };

    private GoogleApiClient.OnConnectionFailedListener mConnFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, "GoogleApiClient Connection Failed. Reason : " + connectionResult.getErrorCode()
            + " Error Code : " + connectionResult.getErrorCode());
        }
    };

    public BaseListener(Context context, String tag, String basePath, List<Class<? extends Annotation>> annotations) {

        listenerClasses = new ArrayList<>();
        objectAnnotationCache = new HashMap<>();
        annotationCollection = new ArrayList<>(annotations);
        TAG = tag;
        BASE_PATH = basePath;

        mMainHandler = new Handler(Looper.getMainLooper());
        HandlerThread handlerThread = new HandlerThread(TAG + "Thread");
        handlerThread.start();
        mBkgndHandler = new Handler(handlerThread.getLooper());

        //Create GoogleApiClient
        googleApiClient = new GoogleApiClient.Builder(context)
                                                        .addApi(Wearable.API)
                                                        .addConnectionCallbacks(mConnCallback)
                                                        .addOnConnectionFailedListener(mConnFailedListener)
                                                        .build();
        googleApiClient.connect();

        nodeHandler = new NodeHandler();
        nodeHandler.setGoogleApiClient(googleApiClient);

        threadPoolExecutor = new ThreadPoolExecutor(MAX_CORES * 2, MAX_CORES * 2, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Adds listener class if it is not already registered
     * @param listener
     */
    public void addListener(Object listener) {

        synchronized (listenerClasses) {
            WeakReference<Object> exist = getExistingByListener(listener);
            if (exist == null) {
                listenerClasses.add(new WeakReference<>(listener));
                objectAnnotationCache.put(listener, getMethodsByAnnotations(listener));
            }
        }

    }

    /**
     * Returns existing listener if already added, otherwise null
     * @param listener
     * @return
     */
    private WeakReference<Object> getExistingByListener(Object listener) {

        WeakReference<Object> exist = null;

        for (WeakReference<Object> existing : listenerClasses) {
            if (existing.get() == listener) {
                exist = existing;
                break;
            }
        }

        return exist;
    }
    /**
     * Unregisters class from listening for the Changes
     * @param listener
     */
    public void removeListener(Object listener) {
        synchronized (listenerClasses) {
            WeakReference<Object> exist = getExistingByListener(listener);
            if (exist != null) {
                listenerClasses.remove(exist);
                objectAnnotationCache.remove(listener);
            }
        }
    }

    protected List<AnnotationProperties> getMethodsByAnnotations(Object listener) {

        List<AnnotationProperties> annotationMethods = new ArrayList<>();

        Log.d(TAG, " Class Details : " + listener.getClass().getName());

        for(Class<? extends Annotation> annotation : annotationCollection) {
           Log.d(TAG, "Available annotations : " + annotation.getName());
        }

        for (Method method : listener.getClass().getDeclaredMethods()) {

            boolean isAnnotationPresent = false;

            for(Class<? extends Annotation> annotation : annotationCollection) {
                if (( isAnnotationPresent = method.isAnnotationPresent(annotation))) {
                    AnnotationProperties properties = new AnnotationProperties();
                    properties.method = method;
                    properties.annotation = method.getAnnotation(annotation);
                    annotationMethods.add(properties);
                    break;
                }
            }

            Log.d(TAG, " Method : " + method.getName() + " IsAnnotation Present : " + isAnnotationPresent);
        }


        return annotationMethods;
    }

    /**
     * Removes recycled listeners
     */
    private void removeInvalidListeners() {

        ArrayList<WeakReference<Object>> recycledListeners = new ArrayList<>();

        for (WeakReference<Object> listener : listenerClasses) {
            if (listener.get() == null) {
                recycledListeners.add(listener);
                objectAnnotationCache.remove(listener);
            }
        }

        if (recycledListeners.size() > 0) {
            synchronized (listenerClasses) {
                listenerClasses.remove(recycledListeners);
            }
        }

    }

    /**
     * Calls catched methods by it's arguement type
     * @param callParams
     */
    protected void callMethodByTarget(Class<? extends Annotation> annotation, String path, final Object... callParams) {

        if (!TextUtils.isEmpty(path) && path.contains(BASE_PATH)) {
            Log.d(TAG, "Fallback message of sent message from the origin. No need to handle this");
            return;
        }

        if (callParams.length > MAX_FUNCTION_PARAMETERS_SUPPORTED) {

            Log.d(TAG, "callMethodByTarget called to invoke unsupported callparam count."
                + " Max supported parameter count : " + MAX_FUNCTION_PARAMETERS_SUPPORTED);

            throw new RuntimeException("callMethodByTarget called to invoke unsupported callparam count."
                    + " Max supported parameter count : " + MAX_FUNCTION_PARAMETERS_SUPPORTED);
        }

        removeInvalidListeners();

        for(final WeakReference<Object> listener : listenerClasses) {

            for(final AnnotationProperties annotationProperties : objectAnnotationCache.get(listener.get())) {

                Class<?> methodParameters[] = annotationProperties.method.getParameterTypes();

                Log.d(TAG, "Method : " + annotationProperties.method.getName()
                        + " Param Count : " + methodParameters.length);

                //Check whether target method contains required annotation
                if (!annotationProperties.method.isAnnotationPresent(annotation)) {
                    continue;
                }

                //Check whether method annotation has targeted any path
                if (!TextUtils.isEmpty(path)) {
                    String annotationPath = getPathFromAnnotation(annotationProperties.annotation);
                    if (!TextUtils.isEmpty(annotationPath) && !path.contains(annotationPath)) {
                        Log.d(TAG, "Path : " + path + " annotationPath : " + annotationPath
                                + " is not present in the annotation " + annotationProperties.annotation);
                        continue;
                    }
                }

                //Check whether same number of parameter count present in target method
                if (callParams.length != methodParameters.length) {
                    Log.d(TAG, "Unable to call target method " + annotationProperties.method.getName()
                            + " as it's " + " parameter count is not matching."
                            + " MethodParameters Count : " + methodParameters.length
                            + " CallParameters Count : " + callParams.length);
                    continue;
                }

                //Check whether same type of parameters present in target method
                int matchMethodParamCount = 0;
                for ( ; matchMethodParamCount < callParams.length; matchMethodParamCount++) {

                    if (!methodParameters[0].isInstance(callParams[0])) {
                        Log.d(TAG, "Unable to call target method " + annotationProperties.method.getName()
                                + " as it's " + " parameter types are not matching calling param type : "
                                + methodParameters[matchMethodParamCount] + " Expected : "
                                + callParams[matchMethodParamCount].getClass());
                        break;
                    }
                }

                if (matchMethodParamCount != callParams.length) {
                    continue;
                }

                //Call Target either Backgroun / Main thread.
                int callingThread = getCallingThread(annotationProperties.annotation);
                Log.d(TAG, "Calling Method on Thread : " + callingThread);

                if (callingThread == AnnotationConstants.BACKGROUND_THREAD
                        && Looper.getMainLooper() == Looper.myLooper()) {

                    mBkgndHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callTarget(annotationProperties, listener, callParams);
                        }
                    });

                } else if (callingThread == AnnotationConstants.MAIN_THREAD
                            && Looper.getMainLooper() != Looper.myLooper()) {

                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callTarget(annotationProperties, listener, callParams);
                        }
                    });

                } else {
                    callTarget(annotationProperties, listener, callParams);
                }
            }
        }
    }

    /**
     * Call Target with parameters
     * @param annotationProperties
     * @param listener
     * @param callParams
     */
    private void callTarget(AnnotationProperties annotationProperties,
                            WeakReference<Object> listener, Object[] callParams) {

        try {
            switch (callParams.length) {
                case 0:
                    annotationProperties.method.invoke(listener.get());
                    break;

                case 1:
                    annotationProperties.method.invoke(listener.get(), callParams[0]);
                    break;

                case 2:
                    annotationProperties.method.invoke(listener.get(), callParams[0], callParams[1]);
                    break;

                case 3:
                    annotationProperties.method.invoke(listener.get(), callParams[0], callParams[1], callParams[2]);
                    break;
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException Method : " + annotationProperties.method
                    + " Object : " + listener);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException Method : " + annotationProperties.method
                    + " Object : " + listener);
            e.printStackTrace();
        }
    }

    /**
     * Returns whether google api client is ready
     * @return
     */
    protected boolean isGoogleAPIClientConnected() {
        return (googleApiClient != null && googleApiClient.isConnected());
    }

    public abstract String getPathFromAnnotation(Annotation annotation);

    public abstract int getCallingThread(Annotation annotation);
}
