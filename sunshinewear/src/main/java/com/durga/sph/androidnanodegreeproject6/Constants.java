package com.durga.sph.androidnanodegreeproject6;

/**
 * Created by root on 12/12/16.
 */

public class Constants {

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
