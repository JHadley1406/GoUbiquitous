package com.hhi.training.ubiquitousweather;

import android.app.Notification;

import com.google.android.gms.wearable.MessageEvent;
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
}
