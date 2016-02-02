
package com.orangelabs.rcs.cms.toolkit;

import android.os.Handler;
import android.os.HandlerThread;

public class ToolkitHandler {

    private static final String THREAD_NAME = "ToolkitThread";

    private static ToolkitHandler sInstance = new ToolkitHandler();

    private final Handler mHandler;

    private ToolkitHandler() {
        HandlerThread thread = new HandlerThread(THREAD_NAME);
        thread.start();
        mHandler = new Handler(thread.getLooper());
    }

    public static ToolkitHandler getInstance() {
        if (sInstance == null) {
            sInstance = new ToolkitHandler();
        }
        return sInstance;
    }

    public Handler getHandler() {
        return mHandler;
    }
}
