package com.example.notes.sync;

public enum SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR_AUTH,
    ERROR_NETWORK,
    ERROR_UNKNOWN
}
