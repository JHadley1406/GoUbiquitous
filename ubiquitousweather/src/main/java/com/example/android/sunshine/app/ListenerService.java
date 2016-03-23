package com.example.android.sunshine.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.common.StringConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by josiahhadley on 3/11/16.
 */
public class ListenerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final String LOG_TAG = ListenerService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private String mHighTemp;
    private String mLowTemp;
    private Asset mIconAsset;

    private Intent mBroadcastIntent;

    @Override
    public void onCreate(){
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();


    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents){

        for(DataEvent event : dataEvents){
            if(event.getType() == DataEvent.TYPE_CHANGED){
                getData(event.getDataItem());
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG_TAG, "In onConnected");
        PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
        results.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                Log.i(LOG_TAG, "Found dataItems");
                if (dataItems.getCount() != 0) {
                    for(DataItem data : dataItems){
                        getData(data);
                    }
                }
            }
        });

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "Connection Failed");
    }

    @Override
    public void onDestroy(){
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    private void getData(DataItem data){
        if(data.getUri().getPath().equals(StringConstants.DATA_SENT)){
            DataMap dataMap = DataMapItem.fromDataItem(data).getDataMap();
            mBroadcastIntent = new Intent();
            new GetBitmap().execute(dataMap.getAsset(StringConstants.WEATHER_ICON));
            mBroadcastIntent.setAction(StringConstants.SEND_WEATHER);
            mBroadcastIntent.putExtra(StringConstants.HIGH_TEMP, dataMap.getString(StringConstants.HIGH_TEMP));
            mBroadcastIntent.putExtra(StringConstants.LOW_TEMP, dataMap.getString(StringConstants.LOW_TEMP));
        }
    }


    private class GetBitmap extends AsyncTask<Asset, Void, Bitmap>{


        @Override
        protected Bitmap doInBackground(Asset... params) {
            return getBitmapFromAsset(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result){
            mBroadcastIntent.putExtra(StringConstants.WEATHER_ICON, result);
            sendBroadcast(mBroadcastIntent);
        }

        private Bitmap getBitmapFromAsset(Asset weatherAsset){
            ConnectionResult result = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
            if(!result.isSuccess()){
                Log.e(LOG_TAG, "could not connect");
            }

            InputStream imageStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, weatherAsset).await().getInputStream();
            return BitmapFactory.decodeStream(imageStream);
        }
    }
}
