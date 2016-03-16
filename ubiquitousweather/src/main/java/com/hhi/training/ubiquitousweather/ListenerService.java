package com.hhi.training.ubiquitousweather;

import android.app.Notification;
import android.content.BroadcastReceiver;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by josiahhadley on 3/11/16.
 */
public class ListenerService extends WearableListenerService {

    private static final String WEATHER_CONSTANT = "foo";
    @Override
    public void onMessageReceived(MessageEvent messageEvent){
        if(messageEvent.getPath().equals(WEATHER_CONSTANT)){
            String data = new String(messageEvent.getData());
            //use Moshi to parse the json sent over
            // use a local broadcast listener to pass the data to the watch face
        } else{
            super.onMessageReceived(messageEvent);
        }
    }

    public void onDataChanged(DataEventBuffer dataEvents){
        DataEvent event = dataEvents.get(0);
        DataItem item = event.getDataItem();
        String path = item.getUri().getPath();
        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
        if(path=="/weatherData"){
            // update watchface with weather data
        }
    }

    public void processRequest(){
        ResultCallback dataCallback = new ResultCallback(){

            @Override
            public void onResult(Result result) {
                // check if data was sent properly
            }
        };
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/getWeather");
        DataMap dataMap = putDataMapRequest.getDataMap();

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
