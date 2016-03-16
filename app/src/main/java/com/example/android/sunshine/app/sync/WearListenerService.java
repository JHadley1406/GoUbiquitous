package com.example.android.sunshine.app.sync;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Josiah Hadley on 3/15/2016.
 */
public class WearListenerService extends WearableListenerService {


    public void processRequest(){
        ResultCallback dataCallback = new ResultCallback(){

            @Override
            public void onResult(Result result) {
                // check if data was sent properly
            }
        };
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/weatherData");
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putInt("high", 99);
        dataMap.putInt("low", 00);
        dataMap.putInt("id", 999);

        PendingResult result = Wearable.DataApi.putDataItem(apiClient, putDataReq);
        result.setResultCallback(dataCallback);
    }

    private void sendMessage(final String message){
        ResultCallback messageCallback = new ResultCallback(){

            @Override
            public void onResult(Result result) {

            }
        };

        ResultCallback nodeCallback = new ResultCallback() {
            @Override
            public void onResult(Result result) {
                public void onResult(NodeApi.GetConnectedNodesResult result){
                    PendingResult pendingResult = Wearable.MessageApi.sendMessage(apiClient, nodeId, message, optionalByteData);
                }
            }
        };

        Wearable.NodeApi.getConnectedNodes(GoogleApiClient).setResultCallback(nodeCallback);


    }
}
