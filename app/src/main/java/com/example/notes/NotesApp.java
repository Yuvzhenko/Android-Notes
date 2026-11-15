package com.example.notes;

import android.app.Application;

import com.example.notes.logging.ReleaseTree;

import timber.log.Timber;

public class NotesApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new ReleaseTree());
        }
        Timber.i("Application started, version %s", BuildConfig.VERSION_NAME);
    }
}
