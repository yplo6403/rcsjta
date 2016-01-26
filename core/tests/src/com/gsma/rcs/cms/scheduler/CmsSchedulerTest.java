package com.gsma.rcs.cms.scheduler;

import android.content.Context;
import android.test.AndroidTestCase;

import com.gsma.rcs.cms.event.CmsEventHandler;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceHandler;
import com.gsma.rcs.cms.integration.RcsSettingsMock;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.ImapLogEnvIntegration;
import com.gsma.rcs.cms.provider.xms.XmsLogEnvIntegration;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.BasicSyncStrategy;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactUtil;

import junit.framework.Assert;

public class CmsSchedulerTest extends AndroidTestCase {

    private static final Logger sLogger = Logger.getLogger(CmsSchedulerTest.class.getSimpleName());

    private Context mContext;
    private LocalStorage mLocalStorage;
    private RcsSettings mSettings;
    private ImapLog mImapLog;
    private XmsLog mXmsLog;

    private CmsSchedulerMock mScheduler;
    private CmsOperationListenerMock mOperationListener;

    private int executionDuration = 100; //in ms

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        ContactUtil.getInstance(mContext);
        mSettings = RcsSettingsMock.getMockSettings(mContext);
        mImapLog = ImapLog.createInstance(mContext);
        mXmsLog = XmsLog.createInstance(mContext, new LocalContentResolver(mContext));
        MessagingLog messagingLog = MessagingLog.createInstance(new LocalContentResolver(mContext),
                mSettings);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, mImapLog, mXmsLog,
                messagingLog, null, mSettings, null);
        mLocalStorage = new LocalStorage(mImapLog, cmsEventHandler);

        mOperationListener = new CmsOperationListenerMock();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        RcsSettingsMock.restoreSettings();
    }

    public void testInitScheduler() throws InterruptedException {

        mSettings.setMessageStoreSyncTimer(500); // periodic sync every 200ms
        mSettings.setDataConnectionSyncTimer(200); // data connection timer 200ms

        mScheduler  = new CmsSchedulerMock(mContext, mSettings, mLocalStorage, mImapLog, mXmsLog);
        mScheduler.setImapServiceHandler(new ImapServiceHandlerMock(mSettings));
        mScheduler.setExecutionDuration(executionDuration);
        mScheduler.registerListener(CmsOperation.SYNC_FOR_DATA_CONNECTION, mOperationListener);
        mScheduler.registerListener(CmsOperation.SYNC_PERIODIC, mOperationListener);
        mScheduler.registerListener(CmsOperation.SYNC_FOR_USER_ACTIVITY, mOperationListener);

        Assert.assertFalse(mScheduler.scheduleSync());

        int lastPeriodicExecution = 0;

        int syncTimerInterval = mSettings.getMessageStoreSyncTimer();

        Assert.assertEquals(0, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_DATA_CONNECTION));
        mScheduler.start();

        Thread.sleep(executionDuration + 100, 0);
        Assert.assertEquals(1, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_DATA_CONNECTION));
        Assert.assertFalse(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_FOR_DATA_CONNECTION.toInt()));
        Assert.assertTrue(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_PERIODIC.toInt()));

        Thread.sleep(executionDuration + syncTimerInterval + 100, 0);
        Assert.assertTrue(mOperationListener.getExecutions(CmsOperation.SYNC_PERIODIC) > lastPeriodicExecution);
        lastPeriodicExecution = mOperationListener.getExecutions(CmsOperation.SYNC_PERIODIC);
        Assert.assertTrue(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_PERIODIC.toInt()));

        Thread.sleep(executionDuration + syncTimerInterval +100, 0);
        Assert.assertTrue(mOperationListener.getExecutions(CmsOperation.SYNC_PERIODIC) > lastPeriodicExecution);
        lastPeriodicExecution = mOperationListener.getExecutions(CmsOperation.SYNC_PERIODIC);
        Assert.assertTrue(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_PERIODIC.toInt()));

        mScheduler.scheduleSync();
        Assert.assertFalse(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_PERIODIC.toInt()));
        Thread.sleep(executionDuration + 200, 0);
        Assert.assertEquals(1, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_USER_ACTIVITY));
        Assert.assertTrue(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_PERIODIC.toInt()));

        mScheduler.stop();
        Assert.assertFalse(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_PERIODIC.toInt()));

    }

    public void testDataConnection() throws InterruptedException {

        mSettings.setMessageStoreSyncTimer(20000); // periodic sync every 20000ms
        mSettings.setDataConnectionSyncTimer(1000); // data connection timer 1000ms

        mScheduler  = new CmsSchedulerMock(mContext, mSettings, mLocalStorage, mImapLog, mXmsLog);
        mScheduler.setImapServiceHandler(new ImapServiceHandlerMock(mSettings));
        mScheduler.setExecutionDuration(executionDuration);
        mScheduler.registerListener(CmsOperation.SYNC_FOR_DATA_CONNECTION, mOperationListener);

        Assert.assertFalse(mScheduler.scheduleSync());

        Assert.assertEquals(0, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_DATA_CONNECTION));
        mScheduler.start();
        Thread.sleep(executionDuration + 100, 0);
        Assert.assertEquals(1, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_DATA_CONNECTION));
        Assert.assertFalse(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_FOR_DATA_CONNECTION.toInt()));
        Assert.assertTrue(mScheduler.mSyncRequestHandler.hasMessages(CmsOperation.SYNC_PERIODIC.toInt()));

        // we simulate a data connection event by stopping and re-starting the scheduler
        mScheduler.stop();
        mScheduler.start();
        Assert.assertEquals(1, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_DATA_CONNECTION));
        Thread.sleep(executionDuration + 100, 0);

        // we simulate a data connection event by stopping and re-starting the scheduler
        mScheduler.stop();
        mScheduler.start();
        Assert.assertEquals(1, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_DATA_CONNECTION));
        Thread.sleep(executionDuration + 100, 0);

        // we simulate a data connection event by stopping and re-starting the scheduler
        mScheduler.stop();
        mScheduler.start();
        Assert.assertEquals(1, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_DATA_CONNECTION));
        Thread.sleep(executionDuration + 1000, 0);

        mScheduler.stop();
        mScheduler.start();
        Thread.sleep(executionDuration + 100, 0);
        Assert.assertEquals(2, mOperationListener.getExecutions(CmsOperation.SYNC_FOR_DATA_CONNECTION));


    }

}
