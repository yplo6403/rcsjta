/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.gsma.rcs.platform.ntp;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

public class NtpManager implements Runnable {

    private static final Logger sLogger = Logger.getLogger(NtpManager.class.getSimpleName());

    private static final String NTP_MANAGER_THREAD_NAME = "NtpManager";
    private static final long POLLING_INTERVAL_IN_MS = 300000; // 5min

    private final Context mContext;
    private final RcsSettings mRcsSettings;

    private Handler mHandler;
    private NtpTrustedTime mNtpTrustedTime;

    private boolean mStarted;

    public NtpManager(Context context, RcsSettings rcsSettings) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mStarted = false;
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    public synchronized void start() {
        if (mStarted) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug(" Start NTP manager");
        }
        mStarted = true;
        mNtpTrustedTime = NtpTrustedTime.getInstance();
        if (mNtpTrustedTime == null) {
            mNtpTrustedTime = NtpTrustedTime.createInstance(mContext, mRcsSettings);
        }

        if (mNtpTrustedTime.getCacheAge() > mRcsSettings.getNtpCacheValidity()) { // sync with NTP
                                                                                  // servers if
                                                                                  // cache
            // has expired
            if (sLogger.isActivated()) {
                sLogger.debug("Cache has expired, schedule new sync with NTP servers");
            }
            mHandler = allocateBgHandler(NTP_MANAGER_THREAD_NAME);
            mHandler.post(this);
            return;
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Data from cache is valid");
            sLogger.debug("--> current trusted time : "
                    + DateUtils.encodeDate(mNtpTrustedTime.currentTimeMillis()));
        }
    }

    public synchronized void stop() {
        if (!mStarted) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug(" Stop NTP manager");
        }

        mStarted = false;

        if (mHandler != null) {
            mHandler.getLooper().quit();
            mHandler.getLooper().getThread().interrupt();
        }
    }

    @Override
    public void run() {
        if (!mNtpTrustedTime.forceRefresh()) { // failed to access NTP server, retry later
            if (sLogger.isActivated()) {
                sLogger.debug("Trusted time has not been refreshed");
                sLogger.debug("--> schedule new sync with NTP server in " + POLLING_INTERVAL_IN_MS
                        + "ms");
            }
            mHandler.postDelayed(this, POLLING_INTERVAL_IN_MS);
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("Trusted time has been successfully refreshed");
                sLogger.debug("--> Cancel background handler");
                sLogger.debug("--> current trusted time : "
                        + DateUtils.encodeDate(mNtpTrustedTime.currentTimeMillis()));
            }
            mHandler.getLooper().quit();
            mHandler = null;
        }
    }
}
