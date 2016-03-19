package com.hhi.training.ubiquitousweather;

import android.graphics.Bitmap;

/**
 * Created by josiahhadley on 3/18/16.
 */
public interface IDisplayCallback {
    void setData(double high, double low, Bitmap weatherImage);
}
