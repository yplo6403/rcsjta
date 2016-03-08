/*
 * Copyright (C) 2011 The Android Open Source Project
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
 */

package com.gsma.rcs.platform.ntp;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

import java.util.List;

public class NtpTrustedTime {

    private final static Logger sLogger = Logger.getLogger(NtpTrustedTime.class.getSimpleName());

    private static NtpTrustedTime sInstance;
    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private ConnectivityManager mCM;
    private boolean mHasCache;
    private long mCachedNtpTime;
    private long mCachedNtpElapsedRealtime;

    private NtpTrustedTime(Context context, RcsSettings rcsSettings) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mHasCache = false;
    }

    public static synchronized NtpTrustedTime createInstance(Context context,
            RcsSettings rcsSettings) {
        if (sInstance == null) {
            sInstance = new NtpTrustedTime(context, rcsSettings);
        }
        return sInstance;
    }

    public static NtpTrustedTime getInstance() {
        return sInstance;
    }

    public boolean forceRefresh() {

        List<String> servers = mRcsSettings.getNtpServers();
        if (servers.isEmpty()) {
            if (sLogger.isActivated()) {
                sLogger.warn("NTP servers list is empty");
            }
            return false;
        }

        // We can't do this at initialization time: ConnectivityService might not be running yet.
        synchronized (this) {
            if (mCM == null) {
                mCM = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            }
        }
        final NetworkInfo ni = mCM == null ? null : mCM.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            if (sLogger.isActivated()) {
                sLogger.debug("forceRefresh: no connectivity");
            }
            return false;
        }

        final SntpClient client = new SntpClient();
        long timeout = mRcsSettings.getNtpServerTimeout();
        for (String server : servers) {
            if (client.requestTime(server, (int) timeout)) {
                mHasCache = true;
                mCachedNtpTime = client.getNtpTime();
                mCachedNtpElapsedRealtime = client.getNtpTimeReference();

                // save local offset in settings
                long localOffset = mCachedNtpTime - System.currentTimeMillis();
                if (sLogger.isActivated()) {
                    sLogger.debug("forceRefresh : save local offset in settings : " + localOffset);
                }
                mRcsSettings.setNtpLocalOffset(localOffset);
                return true;
            }
        }
        return false;
    }

    public long getCacheAge() {
        if (mHasCache) {
            return SystemClock.elapsedRealtime() - mCachedNtpElapsedRealtime;
        } else {
            return Long.MAX_VALUE;
        }
    }

    public static long currentTimeMillis() {
        boolean logActivated = sLogger.isActivated();
        if (sInstance == null) {
            if (logActivated) {
                sLogger.debug("currentTimeMillis : no instance of NtpTrustedTime, return local system time");
            }
            return System.currentTimeMillis();
        }

        if (!sInstance.mHasCache) {
            long localOffset = sInstance.mRcsSettings.getNtpLocalOffset();
            if (logActivated) {
                sLogger.debug("currentTimeMillis : cache not available for NtpTrustedTime, use local offset from settings : "
                        + localOffset);
            }
            return System.currentTimeMillis() + localOffset;
        }

        return sInstance.mCachedNtpTime + sInstance.getCacheAge();
    }
}
