package com.durga.sph.sunshinewear;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by root on 12/12/16.
 */

public class WeatherListenerService extends WearableListenerService {
    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
        for(DataEvent event : dataEventBuffer){
            if(event.getType() == DataEvent.TYPE_CHANGED) {
                DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                String path = event.getDataItem().getUri().getPath();
                if(path.equals(Constants.WEARABLE_DATA)){
                    Log.d("Mysunshine watch face", map.toString());
                    /*String weatherId = dataMap.getString("weatherId");
                    String highTemp = dataMap.getString("highTemp");
                    String lowTemp = dataMap.getString("lowTemp");
                    int timeStamp = dataMap.getInt("timeStamp");


                    Log.d("Listener service:", weatherId + " " + highTemp + " " + lowTemp);
                    */
                }
            }
        }
    }
}
