package com.example.android.sunshine.app.sync;

import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Josiah Hadley on 3/15/2016.
 */
public class WearListenerService extends WearableListenerService {

    private final String LOG_TAG = WearListenerService.class.getSimpleName();
    private final String DATA_SENT = "/send_weather";

    public void processRequest(){
        ResultCallback dataCallback = new ResultCallback<DataApi.DataItemResult>(){

            @Override
            public void onResult(DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess()) {
                    Log.d(LOG_TAG, "Data item set: " + result.getDataItem().getUri());
                }
            }
        };
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .build();

        Cursor cursor = getContentResolver().query(WeatherContract.WeatherEntry.buildWeatherLocation(Utility.getPreferredLocation(getApplicationContext())), null, null, null, null);
        if(cursor.moveToFirst()) {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(DATA_SENT);
            DataMap dataMap = putDataMapRequest.getDataMap();
            double high;
            double low;
            if(Utility.isMetric(getApplicationContext())){
                high = (cursor.getInt(cursor.getColumnIndexOrThrow(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)) * 1.8) + 32;
                low = (cursor.getInt(cursor.getColumnIndexOrThrow(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)) * 1.8) + 32;
            } else {
                high = cursor.getInt(cursor.getColumnIndexOrThrow(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
                low = cursor.getInt(cursor.getColumnIndexOrThrow(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
            }
            dataMap.putDouble("high", high);
            dataMap.putDouble("low", low);
            dataMap.putAsset("icon", getWeatherImage(getResources(), cursor.getInt(cursor.getColumnIndexOrThrow(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID))));
            PendingResult result = Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest());
            result.setResultCallback(dataCallback);
        }

    }

    // toByteArray and closeQuietly borrowed from google's Agent Data wearable example
    // http://developer.android.com/samples/AgendaData/index.html
    private byte[] toByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        closeQuietly(stream);
        return byteArray;
    }

    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException while closing closeable.", e);
        }
    }

    // getWeatherImage modified from getDefaultProfile from google's Agent Data Wearable example
    // http://developer.android.com/samples/AgendaData/index.html
    private Asset getWeatherImage(Resources res, int weatherId) {
        Bitmap bitmap = BitmapFactory.decodeResource(res, Utility.getIconResourceForWeatherCondition(weatherId));
        return Asset.createFromBytes(toByteArray(bitmap));
    }
    /*
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


    }*/
}
