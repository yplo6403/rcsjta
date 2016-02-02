/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.cms.toolkit;

import com.gsma.services.rcs.RcsServiceControl;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.EnumSet;

/**
 * This subclass of Application allows to get a resource content from a static context
 * 
 * @author Philippe LEMORDANT
 */
public class ToolkitApplication extends Application {

    /**
     * Delay (ms) before starting connection manager.
     */
    /* package private */static final long DELAY_FOR_STARTING_CNX_MANAGER = 1000;

    private static Context mContext;

    private static RcsServiceControl mRcsServiceControl;

    /* package private */static boolean sCnxManagerStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        mRcsServiceControl = RcsServiceControl.getInstance(mContext);

        /* Do not execute the ConnectionManager on the main thread */
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        final ConnectionManager cnxManager = ConnectionManager.createInstance(mContext,
                mRcsServiceControl, EnumSet.allOf(RcsServiceName.class));
        mainThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    cnxManager.start();
                    sCnxManagerStarted = true;

                } catch (RuntimeException e) {
                }
            }
        }, DELAY_FOR_STARTING_CNX_MANAGER);
    }

    /**
     * Gets the application context
     * 
     * @return the application context
     */
    public static Context getAppContext() {
        return mContext;
    }

    /**
     * Gets the RCS service control singleton
     * 
     * @return the RCS service control singleton
     */
    public static RcsServiceControl getRcsServiceControl() {
        return mRcsServiceControl;
    }

}
