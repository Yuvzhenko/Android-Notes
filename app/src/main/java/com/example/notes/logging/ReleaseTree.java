package com.example.notes.logging;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import timber.log.Timber;

public class ReleaseTree extends Timber.Tree {

    private static final String DEFAULT_TAG = "Notes";

    @Override
    protected boolean isLoggable(@Nullable String tag, int priority) {
        return priority >= Log.WARN;
    }

    @Override
    protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
        String fullMessage = t == null ? message : message + "\n" + Log.getStackTraceString(t);
        Log.println(priority, tag != null ? tag : DEFAULT_TAG, fullMessage);
    }
}
