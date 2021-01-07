package com.example.blurdialogex.lib;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

public class ApplicationLoader extends Application {
    public static volatile Handler applicationHandler;
    public static volatile Context applicationContext;

    @Override
    public void onCreate() {
        try {
            applicationContext = getApplicationContext();
        } catch (Throwable ignore) {

        }

        super.onCreate();

        if (applicationContext == null) {
            applicationContext = getApplicationContext();
        }

        applicationHandler = new Handler(applicationContext.getMainLooper());

    }
}
