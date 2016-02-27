package com.macastiblancot.lectorsitp.manager;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.macastiblancot.lectorsitp.SITPReaderApplication;

/**
 * Created by Miguel on 2/20/16.
 */
public enum AnalyticsManager {
    INSTANCE;

    Tracker tracker;

    AnalyticsManager(){
        tracker = SITPReaderApplication.getInstance().getDefaultTracker();
        tracker.enableAdvertisingIdCollection(true);
    }

    public void trackScreen(Screen screen){
        tracker.setScreenName(screen.getName());
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void trackAction(Action action){
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction(action.getName())
                .build());
    }

    public enum Screen {
        READ_TAG("ReadTag");

        String name;

        Screen(String name){
            this.name = name;
        }

        public String getName(){
            return name;
        }
    }

    public enum Action {
        READ_TAG("ReadTag"),
        FAILED_READ_TAG("ReadTag"),
        SUCCESSFUL_READ_TAG("ReadTag");


        String name;

        Action(String name){
            this.name = name;
        }

        public String getName(){
            return name;
        }
    }

}
