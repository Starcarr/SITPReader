package com.macastiblancot.lectorsitp;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by Miguel on 2/22/16.
 */
public class SITPReaderApplication extends Application {
    private Tracker mTracker;
    private static SITPReaderApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    public static SITPReaderApplication getInstance() {
        return instance;
    }
}
