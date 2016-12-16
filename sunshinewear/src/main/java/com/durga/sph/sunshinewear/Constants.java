package com.durga.sph.sunshinewear;

/**
 * Created by root on 12/12/16.
 */

public class Constants {
    public static final String WEARABLE_DATA = "/wearable_data";

    public static String getWeekDay(int day)
    {
        if(day == 0) return "SUN";
        if(day == 1) return "MON";
        if(day == 2) return "TUE";
        if(day == 3) return "WED";
        if(day == 4) return "THUR";
        if(day == 5) return "FRI";
        return "SAT";
    }
}
