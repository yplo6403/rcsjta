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
import com.gsma.rcs.core.cms.service.CmsService;
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
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
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
        Context context = getContext();
        ContactUtil.getInstance(getContext());
        RcsSettings settings = RcsSettingsMock.getMockSettings(context);
        AndroidFactory.setApplicationContext(context, settings);
        CmsLog cmsLog = CmsLog.getInstance(context);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(context);
        LocalContentResolver localContentResolver = new LocalContentResolver(context);
        XmsLog xmsLog = XmsLog.getInstance(context, settings, localContentResolver);
        mMessagingLog = MessagingLog.getInstance(localContentResolver, settings);

        InstantMessagingService instantMessagingService = new InstantMessagingService(null,
                settings, null, mMessagingLog, null, localContentResolver, context, null);
        ChatServiceImpl chatService = new ChatServiceImpl(instantMessagingService, mMessagingLog,
                null, settings, null);
        XmsManager xmsManager = new XmsManager(context, context.getContentResolver());
        CmsService cmsService = new CmsService(null, null, context, settings, xmsLog,
                mMessagingLog, cmsLog);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(context, cmsService, chatService,
                xmsLog, settings, xmsManager, localContentResolver);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(context, cmsLog, xmsLog,
                mMessagingLog, chatService, cmsServiceImpl, settings);
        LocalStorage localStorage = new LocalStorage(cmsLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(settings);
        mBasicImapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(context, settings, mBasicImapService, localStorage,
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

        return new StringBuilder()
                .append("Date: Thu, 11 Feb 2016 14:00:49 +0100")
                .append(Constants.CRLF)
                .append("From: tel:+33643209850")
                .append(Constants.CRLF)
                .append("To: sip:Conference-Factory@volteofr.com")
                .append(Constants.CRLF)
                .append("Message-ID: <881999583.1171.1455195649122@RCS5frontox1>")
                .append(Constants.CRLF)
                .append("Subject: cfff")
                .append(Constants.CRLF)
                .append("MIME-Version: 1.0")
                .append(Constants.CRLF)
                .append("Content-Type: Application/X-CPM-Session")
                .append(Constants.CRLF)
                .append("Content-Transfer-Encoding: 8bit")
                .append(Constants.CRLF)
                .append("Conversation-ID: ")
                .append(chatId)
                .append(Constants.CRLF)
                .append("Contribution-ID: ")
                .append(chatId)
                .append(Constants.CRLF)
                .append("IMDN-Message-ID: UFoF32nXQSy5l3d4cVGwZXn4f8YQ8rq6")
                .append(Constants.CRLF)
                .append("Message-Direction: sent")
                .append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append(Constants.CRLF)
                .append("<session>")
                .append("<session-type>Group</session-type>")
                .append("<sdp>o=- 3664184448 3664184448 IN IP4 sip.imsnsn.fr</sdp>")
                .append("<invited-participants>tel:+33642639381;tel:+33643209850</invited-participants>")
                .append("</session>").toString();
    }

}
