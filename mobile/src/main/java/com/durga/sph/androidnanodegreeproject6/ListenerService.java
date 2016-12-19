package com.durga.sph.androidnanodegreeproject6;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.durga.sph.androidnanodegreeproject6.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by root on 12/19/16.
 */

public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    GoogleApiClient mGoogleApiClient;
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();
        Wearable.MessageApi.addListener(mGoogleApiClient,this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if(messageEvent.getPath().equals(getResources().getString(R.string.weather_from_msg_path))){
            Log.d(getClass().getName(), "Message received from wear");
            notifyWearable();
        }
    }

    @Override
    public void onDestroy() {
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void notifyWearable(){
        String location = Utility.getPreferredLocation(this);
        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(location, System.currentTimeMillis());
        Cursor cursor = getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);
        if(cursor != null && cursor.moveToFirst()){
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);
            String description = cursor.getString(INDEX_SHORT_DESC);

            String iconId = String.valueOf(weatherId);
            String highTemp = Utility.formatTemperature(this, high);
            String lowTemp = Utility.formatTemperature(this, low);
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), Utility.getIconResourceForWeatherCondition(weatherId));
            Asset asset = Utility.createAssetFromBitmap(bitmap);
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(getResources().getString(R.string.weather_data)).setUrgent();
            putDataMapRequest.getDataMap().putString(getResources().getString(R.string.high_temp) , highTemp);
            putDataMapRequest.getDataMap().putString(getResources().getString(R.string.low_temp) , lowTemp);
            putDataMapRequest.getDataMap().putAsset(getResources().getString(R.string.weahter_icon_id) , asset);
            putDataMapRequest.getDataMap().putString(getResources().getString(R.string.time) , String.valueOf(System.currentTimeMillis()));
            PutDataRequest request = putDataMapRequest.asPutDataRequest().setUrgent();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                    Log.d(getClass().getName(), dataItemResult.toString());
                }
            });
        }
        cursor.close();
    }
}
