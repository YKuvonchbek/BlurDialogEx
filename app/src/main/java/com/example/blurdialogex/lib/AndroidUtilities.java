package com.example.blurdialogex.lib;

import android.content.Context;

public class AndroidUtilities {

    public static int statusBarHeight = 0;
    public static float density = 1;

    public AndroidUtilities(Context context) {

    }

    public static void fillStatusBarHeight(Context context) {

        density = context.getResources().getDisplayMetrics().density;

        if (context == null || AndroidUtilities.statusBarHeight > 0) {
            return;
        }
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            AndroidUtilities.statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
    }

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (delay == 0) {
            ApplicationLoader.applicationHandler.post(runnable);
        } else {
            ApplicationLoader.applicationHandler.postDelayed(runnable, delay);
        }
    }
}
