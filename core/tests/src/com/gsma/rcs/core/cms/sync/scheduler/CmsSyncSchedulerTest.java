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
 ******************************************************************************/

package com.gsma.rcs.core.cms.sync.scheduler;

import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class CmsSyncSchedulerTest extends AndroidTestCase {

    private LocalStorage mLocalStorage;
    private RcsSettings mSettings;
    private CmsLog mCmsLog;
    private XmsLog mXmsLog;

    private CmsSyncSchedulerMock mScheduler;
    private CmsSyncSchedulerListenerMock mOperationListener;

    private long executionDuration = 100; // in ms

    protected void setUp() throws Exception {
        super.setUp();
        ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mSettings = RcsSettingsMock.getMockSettings(mContext);
        mCmsLog = CmsLog.getInstance(mContext);
        LocalContentResolver localContentResolver = new LocalContentResolver(mContext);
        mXmsLog = XmsLog.getInstance(mContext, mSettings, localContentResolver);
        MessagingLog messagingLog = MessagingLog.getInstance(localContentResolver, mSettings);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, localContentResolver,
                mCmsLog, mXmsLog, messagingLog, null, null, null, null, mSettings);
        mLocalStorage = new LocalStorage(mSettings, mCmsLog, cmsEventHandler);
        mOperationListener = new CmsSyncSchedulerListenerMock();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        RcsSettingsMock.restoreSettings();
        if (mScheduler != null) {
            mScheduler.stop();
        }
    }

    public void testInitScheduler() throws InterruptedException {
        mSettings.setMessageStoreSyncTimer(500); // periodic sync every 200ms
        mSettings.setDataConnectionSyncTimer(200); // data connection timer 200ms
        mScheduler = new CmsSyncSchedulerMock(mContext, mSettings, mLocalStorage, mCmsLog, mXmsLog);
        CmsSyncScheduler.sEndOfLastSync = 0;
        mScheduler.setImapServiceHandler(new ImapServiceHandlerMock(mSettings));
        mScheduler.setExecutionDuration(executionDuration);
        mScheduler.registerListener(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION,
                mOperationListener);
        mScheduler.registerListener(CmsSyncSchedulerTaskType.SYNC_PERIODIC, mOperationListener);
        mScheduler.registerListener(CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY,
                mOperationListener);
        Assert.assertFalse(mScheduler.scheduleSync());
        int lastPeriodicExecution = 0;
        long syncTimerInterval = mSettings.getMessageStoreSyncTimer();
        assertEquals(0,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION));
        mScheduler.start();
        Thread.sleep(executionDuration + 100, 0);
        assertEquals(1,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION));
        Assert.assertFalse(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION.toInt()));
        Assert.assertTrue(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt()));

        Thread.sleep(executionDuration + syncTimerInterval + 100, 0);
        Assert.assertTrue(mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_PERIODIC) > lastPeriodicExecution);
        lastPeriodicExecution = mOperationListener
                .getExecutions(CmsSyncSchedulerTaskType.SYNC_PERIODIC);
        Assert.assertTrue(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt()));

        Thread.sleep(executionDuration + syncTimerInterval + 100, 0);
        Assert.assertTrue(mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_PERIODIC) > lastPeriodicExecution);
        mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_PERIODIC);
        Assert.assertTrue(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt()));

        mScheduler.scheduleSync();
        Assert.assertFalse(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt()));
        Thread.sleep(executionDuration + 200, 0);
        assertEquals(1,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_USER_ACTIVITY));
        Assert.assertTrue(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt()));

        mScheduler.stop();
        Assert.assertFalse(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt()));

    }

    public void testDataConnection() throws InterruptedException {

        mSettings.setMessageStoreSyncTimer(20000); // periodic sync every 20000ms
        mSettings.setDataConnectionSyncTimer(1000); // data connection timer 1000ms

        mScheduler = new CmsSyncSchedulerMock(mContext, mSettings, mLocalStorage, mCmsLog, mXmsLog);
        CmsSyncScheduler.sEndOfLastSync = 0;
        mScheduler.setImapServiceHandler(new ImapServiceHandlerMock(mSettings));
        mScheduler.setExecutionDuration(executionDuration);
        mScheduler.registerListener(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION,
                mOperationListener);

        Assert.assertFalse(mScheduler.scheduleSync());

        assertEquals(0,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION));
        mScheduler.start();
        Thread.sleep(executionDuration + 100, 0);
        assertEquals(1,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION));
        Assert.assertFalse(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION.toInt()));
        Assert.assertTrue(mScheduler.mSyncRequestHandler
                .hasMessages(CmsSyncSchedulerTaskType.SYNC_PERIODIC.toInt()));

        // we simulate a data connection event by stopping and re-starting the scheduler
        mScheduler.stop();
        mScheduler.start();
        assertEquals(1,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION));
        Thread.sleep(executionDuration + 100, 0);

        // we simulate a data connection event by stopping and re-starting the scheduler
        mScheduler.stop();
        mScheduler.start();
        assertEquals(1,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION));
        Thread.sleep(executionDuration + 100, 0);

        // we simulate a data connection event by stopping and re-starting the scheduler
        mScheduler.stop();
        mScheduler.start();
        assertEquals(1,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION));
        Thread.sleep(executionDuration + 1000, 0);

        mScheduler.stop();
        mScheduler.start();
        Thread.sleep(executionDuration + 100, 0);
        assertEquals(2,
                mOperationListener.getExecutions(CmsSyncSchedulerTaskType.SYNC_FOR_DATA_CONNECTION));
    }

}
