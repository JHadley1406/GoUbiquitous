/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hhi.training.ubiquitousweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.wearable.Asset;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {

    // TODO: implement Broadcast Listener to catch broadcasts from the Listener Service to update temp
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements IDisplayCallback{
        private final String LOG_TAG = Engine.class.getSimpleName();
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mTime.clear(intent.getStringExtra("time-zone"));
                //mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mHandPaint;
        Paint mTextPaint;

        SimpleDateFormat mTextTimeFormat;

        boolean mAmbient;
        boolean mIsRound;
        Time mTime;
        Date mDate;
        Calendar mCal;
        int mTapCount;
        String mTemps;
        Bitmap mWeatherIcon;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = MyWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mTextPaint = new Paint();
            mTextPaint.setColor(resources.getColor(R.color.analog_hands));
            mTextPaint.setAntiAlias(true);

            mTextTimeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());

            mTemps = "32" + (char) 0x00B0 + "/78" + (char) 0x00B0;
            mWeatherIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_clear);

            mTime = new Time();

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MyWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets){
            super.onApplyWindowInsets(insets);
            if(insets.isRound()){
                mIsRound = true;
            } else {
                mIsRound = false;
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();
            mTextPaint.setTextSize(bounds.width()/15);

            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
                canvas.drawText(mTemps, (centerY + (centerY / 2)) - (mTemps.length() * 7), centerX + (centerX / 2), mTextPaint);
                canvas.drawBitmap(mWeatherIcon
                                , centerY*2
                                , centerX/2
                                , mBackgroundPaint
                );

            }
            if(mIsRound) {
                // Find the center. Ignore the window insets so that, on round watches with a
                // "chin", the watch face is centered on the entire screen, not just the usable
                // portion.

                Log.i(LOG_TAG, "Watchface is Round");
                float secRot = mTime.second / 30f * (float) Math.PI;
                int minutes = mTime.minute;
                float minRot = minutes / 30f * (float) Math.PI;
                float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

                float secLength = centerX - 20;
                float minLength = centerX - 40;
                float hrLength = centerX - 80;

                if (!mAmbient) {
                    float secX = (float) Math.sin(secRot) * secLength;
                    float secY = (float) -Math.cos(secRot) * secLength;
                    canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mHandPaint);
                }

                float minX = (float) Math.sin(minRot) * minLength;
                float minY = (float) -Math.cos(minRot) * minLength;
                canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mHandPaint);

                float hrX = (float) Math.sin(hrRot) * hrLength;
                float hrY = (float) -Math.cos(hrRot) * hrLength;
                canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHandPaint);
            } else {
                Paint mTimePaint = mHandPaint;
                mTimePaint.setTextSize(bounds.width()/6);
                mTimePaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(DateFormat.getTimeInstance().format(new Date()), centerX, centerY, mTimePaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        public void setData(double high, double low, Bitmap icon){
            mWeatherIcon = icon;
            mTemps = low + (char) 0x00B0 + "/" + high + (char) 0x00B0;

        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }


}
