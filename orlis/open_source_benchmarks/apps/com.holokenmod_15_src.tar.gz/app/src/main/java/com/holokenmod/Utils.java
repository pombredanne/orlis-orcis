package com.holokenmod;

import android.os.Build;

public class Utils {
    public static String convertTimetoStr(long time) {
        int seconds = (int) (time / 1000);
        int minutes = seconds / 60 % 60;
        int hours   = seconds / 3600;
        seconds     = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

}