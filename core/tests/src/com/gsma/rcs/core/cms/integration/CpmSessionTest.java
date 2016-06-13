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

package com.gsma.rcs.core.cms.integration;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncDeleteTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncDeleteTask.Operation;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsLogTestIntegration;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;

import android.test.AndroidTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class CpmSessionTest extends AndroidTestCase {

    private ImapServiceHandler mImapServiceHandler;
    private BasicImapService mBasicImapService;
    private BasicSyncStrategy mSyncStrategy;
    private MessagingLog mMessagingLog;
    private CmsLogTestIntegration mCmsLogTestIntegration;

    protected void setUp() throws Exception {
        super.setUp();
        RcsSettings settings = RcsSettingsMock.getMockSettings(mContext);
        AndroidFactory.setApplicationContext(mContext, settings);
        CmsLog cmsLog = CmsLog.getInstance(mContext);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(mContext);
        LocalContentResolver localContentResolver = new LocalContentResolver(mContext);
        XmsLog xmsLog = XmsLog.getInstance(mContext, settings, localContentResolver);
        mMessagingLog = MessagingLog.getInstance(localContentResolver, settings);
        CmsSessionController cmsSessionCtrl = new CmsSessionController(mContext, null, null,
                settings, localContentResolver, xmsLog, mMessagingLog, cmsLog);
        InstantMessagingService imService = new InstantMessagingService(null, settings, null,
                mMessagingLog, null, localContentResolver, mContext, null, cmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, mMessagingLog, null, settings,
                null, cmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, mMessagingLog, settings, null, mContext, cmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, mContext.getContentResolver());

        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, cmsSessionCtrl, chatService,
                fileTransferService, imService, xmsLog, settings, xmsManager, localContentResolver);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, localContentResolver,
                cmsLog, xmsLog, mMessagingLog, chatService, fileTransferService, cmsServiceImpl,
                imService, settings);
        LocalStorage localStorage = new LocalStorage(settings, cmsLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(settings);
        mBasicImapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(mContext, settings, mBasicImapService, localStorage,
                xmsLog, cmsLog);
        mBasicImapService.init();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapServiceHandler.closeService();
        RcsSettingsMock.restoreSettings();
    }

    /**
     * Test1 step 0 : purge local storage and CMS server folders step 1 : create a conversation on
     * CMS server step 2 : start a sync
     */
    public void testSyncCpmSession() throws Exception {
        // create messages on CMS
        String chatId = UUID.randomUUID().toString();
        String remoteFolder = "Default/" + chatId + "/" + chatId;
        createRemoteMessage(remoteFolder, getCpmSessionPayload(chatId));

        int initialNbMessages = mCmsLogTestIntegration.getMessages(remoteFolder).size();
        assertFalse(mMessagingLog.isGroupChatPersisted(chatId));
        // start synchro
        startSynchro();

        assertEquals(initialNbMessages + 1, mCmsLogTestIntegration.getMessages(remoteFolder).size());
        assertTrue(mMessagingLog.isGroupChatPersisted(chatId));

        deleteRemoteMailbox(remoteFolder);
        deleteRemoteMailbox("Default/" + chatId);
    }

    private void createRemoteMessage(String remoteFolder, String payload) throws Exception {
        createRemoteMessages(remoteFolder, new String[] {
            payload
        });
    }

    private void deleteRemoteMailbox(String mailbox) throws Exception {
        CmsSyncDeleteTask deleteTask = new CmsSyncDeleteTask(Operation.DELETE_MAILBOX, mailbox,
                null);
        deleteTask.delete(mBasicImapService, mailbox);
        try {
            mBasicImapService.close();
        } catch (IOException ignore) {
        }
        mBasicImapService.init();
    }

    private void createRemoteMessages(String remoteFolder, String[] payloads) throws Exception {
        mBasicImapService.create(remoteFolder);
        mBasicImapService.selectCondstore(remoteFolder);
        for (String payload : payloads) {
            mBasicImapService.append(remoteFolder, new ArrayList(), payload);
        }
    }

    private void startSynchro() throws Exception {
        mSyncStrategy.execute();
    }

    public String getCpmSessionPayload(String chatId) {
        return "Date: Thu, 11 Feb 2016 14:00:49 +0100" + Constants.CRLF + "From: tel:+33643209850"
                + Constants.CRLF + "To: sip:Conference-Factory@volteofr.com" + Constants.CRLF
                + "Message-ID: <881999583.1171.1455195649122@RCS5frontox1>" + Constants.CRLF
                + "Subject: cfff" + Constants.CRLF + "MIME-Version: 1.0" + Constants.CRLF
                + "Content-Type: application/x-cpm-session" + Constants.CRLF
                + "Content-Transfer-Encoding: 8bit" + Constants.CRLF + "Conversation-ID: " + chatId
                + Constants.CRLF + "Contribution-ID: " + chatId + Constants.CRLF
                + "IMDN-Message-ID: UFoF32nXQSy5l3d4cVGwZXn4f8YQ8rq6" + Constants.CRLF
                + "Message-Direction: sent" + Constants.CRLF + Constants.CRLF
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + Constants.CRLF + "<session>"
                + "<session-type>Group</session-type>"
                + "<sdp>o=- 3664184448 3664184448 IN IP4 sip.imsnsn.fr</sdp>"
                + "<invited-participants>tel:+33642639381;tel:+33643209850</invited-participants>"
                + "</session>";
    }

}
