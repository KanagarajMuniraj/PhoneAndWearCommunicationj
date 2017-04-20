package kanagaraj.wearcommunication.util;

import android.text.TextUtils;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by kanagaraj on 10/4/17.
 * It searches the node by it's capability if set otherwise all connected nodes and if they are near
 * by
 */
public class NodeHandler {

    private static NodeHandler _me;
    private SoftReference<GoogleApiClient> mGoogleApiClient;
    private String mCapabilityName;

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        mGoogleApiClient = new SoftReference<GoogleApiClient>(googleApiClient);
    }

    public void addCapabilityName(String capabilityName) {
        mCapabilityName = capabilityName;
    }

    /**
     * This function must be called from inside a thread as it contains blocker call
     * @return
     */
    public List<Node> getAvailableNodes() {

        List<Node> connectedNodes = new ArrayList<>();

        GoogleApiClient googleApiClient = null;

        if (mGoogleApiClient == null || (googleApiClient = mGoogleApiClient.get()) == null) {
            return null;
        }

        //If google api client is not connected, try connecting it.
        if (!googleApiClient.isConnected()) {
            googleApiClient.blockingConnect(Constants.GOOGLE_API_CLIENT_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        }

        if (!googleApiClient.isConnected()) {
            //Unable to connect to google api client. Can't proceed.
            throw new RuntimeException("Unable to connect to GoogleAPIClient.");
        }

        if (!TextUtils.isEmpty(mCapabilityName)) {
            CapabilityApi.GetCapabilityResult result =
                    Wearable.CapabilityApi.getCapability(
                            mGoogleApiClient.get(), mCapabilityName,
                            CapabilityApi.FILTER_REACHABLE).await();
            connectedNodes.addAll(getNearbyNodes(result.getCapability().getNodes()));
        } else {
            List<Node> pairedNodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient.get())
                    .await(Constants.NODE_RETRIEVE_TIMEOUT, TimeUnit.MILLISECONDS).getNodes();
            connectedNodes.addAll(getNearbyNodes(pairedNodes));
        }

        return connectedNodes;
    }

    /**
     * Returns only near by nodes
     * @param nodes
     * @return
     */
    private List<Node> getNearbyNodes(Collection<Node> nodes) {

        List<Node> filteredNodes = new ArrayList<>();

        if (nodes == null) {
            return filteredNodes;
        }

        for (Node node : nodes) {
            if (node != null && node.isNearby()) {
                filteredNodes.add(node);
            }
        }

        return filteredNodes;
    }
}
