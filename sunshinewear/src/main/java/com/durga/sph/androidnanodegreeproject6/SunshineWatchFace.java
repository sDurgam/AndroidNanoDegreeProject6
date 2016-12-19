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

package com.durga.sph.androidnanodegreeproject6;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.durga.sph.androidnanodegreeproject6.Constants.getWeekDay;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {

    /*
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

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                invalidate();
            }
        };

        private final BroadcastReceiver mWeatherUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(getResources().getString(R.string.weather_updated))) {
                    detail_high_textview.setText(intent.getStringExtra(getResources().getString(R.string.high_temp)));
                    detail_low_textview.setText(intent.getStringExtra(getResources().getString(R.string.low_temp)));
                    detail_icon.setImageBitmap((Bitmap) intent.getParcelableExtra(getResources().getString(R.string.weahter_icon_id)));
                    invalidate();
                }
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        float mXOffset = 0;
        float mYOffset = 0;
        private int specW, specH;
        Time mTime;
        private View mView;
        private final Point displaySize = new Point();


        TextView detail_time_textview;
        TextView detail_date_textview;
        ImageView detail_icon;
        TextView detail_high_textview;
        TextView detail_low_textview;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            WeatherListenerService mobileListenerService = new WeatherListenerService();
            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            mTime = new Time();
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = inflater.inflate(R.layout.today_weather_layout, null);
            Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            display.getSize(displaySize);
            specW = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                    View.MeasureSpec.EXACTLY);
            specH = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                    View.MeasureSpec.EXACTLY);
            detail_time_textview = (TextView) mView.findViewById(R.id.detail_time_textview);
            detail_date_textview = (TextView) mView.findViewById(R.id.detail_date_textview);
            detail_icon = (ImageView) mView.findViewById(R.id.detail_icon);
            detail_high_textview = (TextView) mView.findViewById(R.id.detail_high_textview);
            detail_low_textview = (TextView) mView.findViewById(R.id.detail_low_textview);
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
            mAmbient = inAmbientMode;

            updateWatchHandStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {


            } else {

            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            if (insets.isRound()) {
                // Shrink the face to fit on a round screen
                mYOffset = mXOffset = displaySize.x * 0.1f;
                displaySize.y -= 2 * mXOffset;
                displaySize.x -= 2 * mXOffset;
            } else {
                mXOffset = mYOffset = 0;
            }

            // Recompute the MeasureSpec fields - these determine the actual size of the layout
            specW = View.MeasureSpec.makeMeasureSpec(displaySize.x, View.MeasureSpec.EXACTLY);
            specH = View.MeasureSpec.makeMeasureSpec(displaySize.y, View.MeasureSpec.EXACTLY);
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if(isInAmbientMode()){
                detail_icon.setVisibility(View.INVISIBLE);
                detail_high_textview.setVisibility(View.INVISIBLE);
                detail_low_textview.setVisibility(View.INVISIBLE);
                canvas.drawColor(Color.BLACK);
            }
            else {
                mTime.setToNow();
                SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy");
                detail_icon.setVisibility(View.VISIBLE);
                detail_high_textview.setVisibility(View.VISIBLE);
                detail_low_textview.setVisibility(View.VISIBLE);
                detail_time_textview.setText(mTime.hour + ":" + mTime.minute);
                detail_date_textview.setText(getWeekDay(mTime.weekDay) + ", " + df.format(new Date()));
                mView.measure(specW, specH);
                mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
                mView.setVisibility(View.VISIBLE);
                canvas.drawColor(getResources().getColor(R.color.primary));
                canvas.translate(mXOffset, mYOffset);
            }
            mView.draw(canvas);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }
            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
            IntentFilter wetherUpdateFilter = new IntentFilter(getResources().getString(R.string.weather_updated));
            SunshineWatchFace.this.registerReceiver(mWeatherUpdateReceiver, wetherUpdateFilter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            SunshineWatchFace.this.unregisterReceiver(mWeatherUpdateReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
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
    }
}
