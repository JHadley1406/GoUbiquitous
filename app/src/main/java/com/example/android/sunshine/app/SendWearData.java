package com.example.android.sunshine.app;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.common.StringConstants;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;



/**
 * Created by Josiah Hadley on 3/15/2016.
 */
public class SendWearData implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private final String LOG_TAG = SendWearData.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private Cursor mCursor;
    private String mLow;
    private String mHigh;
    private int mWeatherId;


    public SendWearData(Context context){
        mContext = context;

    }

    public void setData(Cursor cursor){
        mCursor = cursor;
        if(cursor.moveToFirst()) {
            mLow = Utility.formatTemperature(mContext
                    , cursor.getDouble(cursor
                    .getColumnIndexOrThrow(WeatherContract
                            .WeatherEntry.COLUMN_MIN_TEMP)));
            mHigh = Utility.formatTemperature(mContext
                    , cursor.getDouble(cursor
                    .getColumnIndexOrThrow(WeatherContract
                            .WeatherEntry.COLUMN_MAX_TEMP)));
            mWeatherId = cursor.getInt(cursor
                    .getColumnIndexOrThrow(WeatherContract
                            .WeatherEntry.COLUMN_WEATHER_ID));
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApiIfAvailable(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    private void processRequest(){

        ResultCallback dataCallback = new ResultCallback<DataApi.DataItemResult>(){

            @Override
            public void onResult(DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess() && result.getDataItem() != null) {
                    Log.d(LOG_TAG, "Data item set: " + result.getStatus());
                }
                mGoogleApiClient.disconnect();
            }
        };
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(StringConstants.DATA_SENT);
        DataMap dataMap = putDataMapRequest.getDataMap();

        dataMap.putString(StringConstants.HIGH_TEMP, mHigh);
        dataMap.putString(StringConstants.LOW_TEMP, mLow);
        dataMap.putAsset(StringConstants.WEATHER_ICON
                , getWeatherImage(mContext.getResources(), mWeatherId));
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(dataCallback);
    }

    // toByteArray and closeQuietly borrowed from Google's Agent Data wearable example
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
        Log.i(LOG_TAG, "Weather ID : " + weatherId);
        Bitmap bitmap = BitmapFactory.decodeResource(res
                    , Utility.getIconResourceForWeatherCondition(weatherId));
        return Asset.createFromBytes(toByteArray(bitmap));
    }

    @Override
    public void onConnected(Bundle bundle) {
        processRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Connection Failed");
    }

}
