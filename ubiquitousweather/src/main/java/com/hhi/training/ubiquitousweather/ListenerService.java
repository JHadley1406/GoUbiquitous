package com.hhi.training.ubiquitousweather;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
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

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by josiahhadley on 3/11/16.
 */
public class ListenerService extends WearableListenerService {

    private final String LOG_TAG = ListenerService.class.getSimpleName();

    private static final String WEATHER_CONSTANT = "foo";
    private final String DATA_ITEM_RECIEVED_PATH = "/data_item_received";
    private final String DATA_SENT = "/send_weather";

    private GoogleApiClient mGoogleApiClient;
    private IDisplayCallback mCallback;


    public ListenerService(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();


    }


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

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        ConnectionResult result = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if(!result.isSuccess()){
            Log.e(LOG_TAG, "Could not connect");
        }

        for(DataEvent event : events){
            DataItem item = event.getDataItem();
            Uri uri = item.getUri();
            String node = uri.getHost();
            //String path = item.getUri().getPath();
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
            Log.i(LOG_TAG, "High : " + dataMap.getInt("high") + " Low : " + dataMap.getInt("Low"));
            if(uri.equals(DATA_SENT)){
                // notify app that we got the data
                byte[] payload = uri.toString().getBytes();
                Wearable.MessageApi.sendMessage(mGoogleApiClient, node, DATA_ITEM_RECIEVED_PATH, payload);
                // update watchface with weather data
                mCallback.setData(dataMap.getDouble("high")
                        , dataMap.getDouble("low")
                        , getBitmapFromAsset(dataMap.getAsset("icon")));
            }
        }
    }

    private Bitmap getBitmapFromAsset(Asset weatherAsset){
        ConnectionResult result = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        if(!result.isSuccess()){
            Log.e(LOG_TAG, "could not connect");
        }

        InputStream imageStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, weatherAsset).await().getInputStream();
        return BitmapFactory.decodeStream(imageStream);
    }
/*
    private void sendMessage(final String message){

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ResultCallback nodeCallback = new ResultCallback() {
            @Override
            public void onResult(Result result) {
                public void onResult(NodeApi.GetConnectedNodesResult result){
                    PendingResult pendingResult = Wearable.MessageApi.sendMessage(googleApiClient, nodeId, message, optionalByteData);
                }
            }
        };



        Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(nodeCallback);


    }*/
}
