package com.example.kilgi.inventory.data.common;

import android.os.Handler;
import android.os.Looper;

public class AppExecutors {
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable runnable) {
        mainThreadHandler.post(runnable);
    }
}
