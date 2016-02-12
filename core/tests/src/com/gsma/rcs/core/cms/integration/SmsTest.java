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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test1;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test2;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test7;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test8;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.Test9;
import com.gsma.rcs.core.cms.integration.SmsIntegrationUtils.TestLoad;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsService;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.FlagChange;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncDeleteTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncDeleteTask.Operation;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncPushMessageTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncUpdateFlagTask;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsLogTestIntegration;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.PartData;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SmsTest extends AndroidTestCase {

    private XmsLogEnvIntegration mXmsLogEnvIntegration;
    private RcsSettings mSettings;
    private ImapServiceHandler mImapServiceHandler;
    private BasicImapService mBasicImapService;
    private BasicSyncStrategy mSyncStrategy;
    private CmsLog mCmsLog;
    private CmsLogTestIntegration mCmsLogTestIntegration;
    private XmsLog mXmsLog;
    private LocalContentResolver mLocalContentResolver;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContactUtil.getInstance(getContext());
        mSettings = RcsSettingsMock.getMockSettings(context);
        AndroidFactory.setApplicationContext(context, mSettings);
        mCmsLog = CmsLog.getInstance(context);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(context);
        mLocalContentResolver = new LocalContentResolver(context);
        mXmsLog = XmsLog.getInstance(context, mSettings, mLocalContentResolver);
        MessagingLog messagingLog = MessagingLog.getInstance(new LocalContentResolver(context),
                mSettings);
        mXmsLogEnvIntegration = XmsLogEnvIntegration.getInstance(context);
        InstantMessagingService instantMessagingService = new InstantMessagingService(null,
                mSettings, null, messagingLog, null, mLocalContentResolver, context, null);
        ChatServiceImpl chatService = new ChatServiceImpl(instantMessagingService, messagingLog,
                null, mSettings, null);
        XmsManager xmsManager = new XmsManager(context, context.getContentResolver());
        CmsService cmsService = new CmsService(null, null, context, mSettings, mXmsLog,
                messagingLog, mCmsLog);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(context, cmsService, chatService,
                mXmsLog, mSettings, xmsManager, mLocalContentResolver);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(context, mCmsLog, mXmsLog,
                messagingLog, chatService, cmsServiceImpl, mSettings);
        LocalStorage localStorage = new LocalStorage(mCmsLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(mSettings);
        mBasicImapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(context, mSettings, mBasicImapService, localStorage);
        mBasicImapService.init();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapServiceHandler.closeService();
        RcsSettingsMock.restoreSettings();
    }

    /**
     * Test1
     * <ul>
     * <li>step 0 : purge local storage and CMS server folders</li>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : start a sync</li>
     * </ul>
     */
    public void test1() throws FileAccessException, NetworkException, PayloadException {
        Map<Integer, CmsObject> imapData;
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(SmsIntegrationUtils.Test1.conversation);

        // start synchro
        startSynchro();

        // check that messages are present in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        int nbFolders = mCmsLog.getFolders().size();
        assertTrue(nbFolders > 0);

        imapData = mCmsLogTestIntegration.getMessages(SmsIntegrationUtils.Test1.folderName);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, imapData.size());

        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, messages.size());
        for (XmsDataObject message : messages) {
            Assert.assertEquals(Test1.readStatus, message.getReadStatus());
        }

        // start synchro : all is up to date
        startSynchro();

        // check that messages are present in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        assertEquals(nbFolders, mCmsLog.getFolders().size());

        imapData = mCmsLogTestIntegration.getMessages(SmsIntegrationUtils.Test1.folderName);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, imapData.size());
    }

    /**
     * Test2 Test 1 + Step 0 : mark conversation as seen on CMS
     * <ul>
     * <li>step 1 : start a sync : messages are marked as seen in local storage</li>
     * <li>Step 2 : mark conversation as deleted on CMS</li>
     * <li>step 3 : start a sync : messages are marked as deleted in local storage</li>
     * </ul>
     */
    public void test2() throws FileAccessException, NetworkException, PayloadException {
        test1();

        // update messages with 'seen' flag on CMS
        updateRemoteFlags(Arrays.asList(Test2.flagChangesSeen));

        // sync with CMS
        startSynchro();

        // check that messages are marked as 'Seen' in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        int nbFolders = mCmsLog.getFolders().size();
        assertTrue(nbFolders > 0);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName).size());
        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, messages.size());
        for (XmsDataObject message : messages) {
            Assert.assertEquals(ReadStatus.READ, message.getReadStatus());
        }

        // update messages with 'deleted' flag on CMS
        updateRemoteFlags(Arrays.asList(Test2.flagChangesDeleted));

        // sync with CMS
        startSynchro();

        // check that messages are marked as 'Deleted' in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        assertEquals(nbFolders, mCmsLog.getFolders().size());
        assertEquals(SmsIntegrationUtils.Test1.conversation.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName).size());
        messages = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(0, messages.size());
    }

    /**
     * Test3 Test 1 + Step 0 : mark conversation as read_report_requested in local storage
     * <ul>
     * <li>Step 1 : start sync : message are marked as seen in CMS</li>
     * <li>step 2 : start sync : messages are marked as seen in local storage</li>
     * <li>Step 3 : mark conversation as deleted_requested in local storage</li>
     * <li>Step 4 : start sync : message are marked as deleted in CMS</li>
     * <li>step 5 : start sync : messages are marked as deleted in local storage</li>
     * </ul>
     */
    public void test3() throws FileAccessException, NetworkException, PayloadException {
        test1();

        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateReadStatus(MessageType.SMS, sms.getMessageId(),
                    CmsObject.ReadStatus.READ_REPORT_REQUESTED);
        }

        // sync with CMS : during this first sync, messages are marked as 'Seen' on CMS
        startSynchro();

        String folder = CmsUtils.contactToCmsFolder(mSettings, SmsIntegrationUtils.Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsObject.ReadStatus.READ, cmsObject.getReadStatus());
        }

        messages = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateDeleteStatus(MessageType.SMS, sms.getMessageId(),
                    CmsObject.DeleteStatus.DELETED_REPORT_REQUESTED);
        }

        // sync with CMS : during this first sync, messages are marked as 'Deleted' on CMS
        startSynchro();

        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test4 Test 1 + Step 0 : delete mailbox from CMS
     * <ul>
     * <li>Step 1 : mark conversation as seen_requested in local storage</li>
     * <li>Step 2 : start sync</li>
     * <li>step 3 : check that conversation is marked as seen</li>
     * </ul>
     */
    public void test4() throws FileAccessException, NetworkException, PayloadException {
        test1();
        // delete mailbox on CMS
        try {
            deleteRemoteMailbox(CmsUtils.contactToCmsFolder(mSettings,
                    SmsIntegrationUtils.Test1.contact));
        } catch (Exception e) {
        }

        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateReadStatus(MessageType.SMS, sms.getMessageId(),
                    CmsObject.ReadStatus.READ_REPORT_REQUESTED);
        }

        startSynchro();

        String folder = CmsUtils.contactToCmsFolder(mSettings, SmsIntegrationUtils.Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsObject.ReadStatus.READ_REPORT_REQUESTED,
                    cmsObject.getReadStatus());
        }

        for (XmsDataObject sms : messages) {
            mCmsLog.updateDeleteStatus(MessageType.SMS, sms.getMessageId(),
                    CmsObject.DeleteStatus.DELETED_REPORT_REQUESTED);
        }

        startSynchro();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test5
     * <ul>
     * <li>Step 1 : mark conversation as seen_requested in local storage</li>
     * <li>Step 2 : start sync</li>
     * <li>step 3 : check that conversation is marked as seen</li>
     * </ul>
     */
    public void test5() throws FileAccessException, NetworkException, PayloadException {
        test1();

        // mark messages as deleted on server and expunge them.
        try {
            updateRemoteFlags(Arrays.asList(SmsIntegrationUtils.Test5.flagChangesDeleted));
            deleteRemoteMessages(CmsUtils.contactToCmsFolder(mSettings,
                    SmsIntegrationUtils.Test1.contact));
        } catch (Exception e) {
            Assert.fail();
        }

        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateReadStatus(MessageType.SMS, sms.getMessageId(),
                    CmsObject.ReadStatus.READ_REPORT_REQUESTED);
        }

        startSynchro();

        String folder = CmsUtils.contactToCmsFolder(mSettings, SmsIntegrationUtils.Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsObject.ReadStatus.READ, cmsObject.getReadStatus());
        }

        for (XmsDataObject sms : messages) {
            mCmsLog.updateDeleteStatus(MessageType.SMS, sms.getMessageId(),
                    CmsObject.DeleteStatus.DELETED_REPORT_REQUESTED);
        }

        startSynchro();

        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test6 : check correlation algorithm (messages having different content)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test6() throws FileAccessException, NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(SmsIntegrationUtils.Test1.conversation);

        // create messages in local storage
        for (SmsDataObject sms : SmsIntegrationUtils.Test1.conversation) {
            mXmsLog.addSms(sms);
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                    sms.getContact()), CmsObject.ReadStatus.READ, DeleteStatus.NOT_DELETED,
                    PushStatus.PUSHED, MessageType.SMS, sms.getMessageId(), null));

        }

        startSynchro();

        assertEquals(SmsIntegrationUtils.Test1.conversation.length, mXmsLogEnvIntegration
                .getMessages(MimeType.TEXT_MESSAGE, SmsIntegrationUtils.Test1.contact).size());
    }

    /**
     * Test6 : check correlation algorithm (messages having the same content)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test7() throws FileAccessException, NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(SmsIntegrationUtils.Test7.conversation);

        // create messages in local storage
        for (SmsDataObject sms : SmsIntegrationUtils.Test7.conversation) {
            mXmsLog.addSms(sms);
            String messageId = sms.getMessageId();
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                    sms.getContact()), Test7.imapReadStatus, Test7.imapDeleteStatus,
                    PushStatus.PUSHED, MessageType.SMS, messageId, null));
        }

        startSynchro();

        List<XmsDataObject> sms = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        assertEquals(SmsIntegrationUtils.Test7.conversation.length, sms.size());
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName);
        assertEquals(SmsIntegrationUtils.Test7.conversation.length, imapData.size());
        Assert.assertEquals(imapData.get(4).getMessageId(), sms.get(0).getMessageId());
        Assert.assertEquals(imapData.get(3).getMessageId(), sms.get(1).getMessageId());
        Assert.assertEquals(imapData.get(2).getMessageId(), sms.get(2).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), sms.get(3).getMessageId());
    }

    /**
     * Test8 : check correlation algorithm (messages having the same content)<br>
     * The local storage has 3 messages (with the same content) whereas the CMS has only 2 messages<br>
     * --> The first local message will not be mapped with a message from CMS<br>
     * --> No download of message from CMS (imap network trace)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test8() throws FileAccessException, NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(SmsIntegrationUtils.Test8.conversation_remote);

        // create messages in local storage
        for (SmsDataObject sms : SmsIntegrationUtils.Test8.conversation_local) {
            mXmsLog.addSms(sms);
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                    sms.getContact()), Test8.imapReadStatus, Test8.imapDeleteStatus,
                    PushStatus.PUSHED, MessageType.SMS, sms.getMessageId(), null));

        }

        startSynchro();

        List<XmsDataObject> sms = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName);

        Assert.assertEquals(Test8.conversation_local.length, sms.size());
        Assert.assertEquals(Test8.conversation_local.length, imapData.size());
        Assert.assertEquals(imapData.get(2).getMessageId(), sms.get(0).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), sms.get(1).getMessageId());
    }

    /**
     * Test9 : check correlation algorithm (messages having the same content) <br>
     * The local storage has 2 messages (with the same content) whereas the CMS has 2 messages with
     * different content<br>
     * --> The first local message will not be mapped with a message from CMS<br>
     * --> One message should be downloaded from CMS (imap network trace)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local</li>
     * <li>storage step 3 : start a sync</li>
     * </ul>
     */
    public void test9() throws FileAccessException, NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(SmsIntegrationUtils.Test9.conversation_remote);

        // create messages in local storage
        for (SmsDataObject sms : SmsIntegrationUtils.Test9.conversation_local) {
            mXmsLog.addSms(sms);
            String messageId = sms.getMessageId();
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                    sms.getContact()), Test9.imapReadStatus, Test9.imapDeleteStatus,
                    PushStatus.PUSHED, MessageType.SMS, messageId, null));
        }

        startSynchro();

        List<XmsDataObject> sms = mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                SmsIntegrationUtils.Test1.contact);
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test1.folderName);

        Assert.assertEquals(2 + 1, sms.size());
        Assert.assertEquals(2 + 1, imapData.size());
        Assert.assertEquals(imapData.get(2).getMessageId(), sms.get(0).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), sms.get(1).getMessageId());
        Assert.assertEquals(imapData.get(null).getMessageId(), sms.get(2).getMessageId());
    }

    /**
     * Test10 : multi contact
     */
    public void test10() throws FileAccessException, NetworkException, PayloadException {

        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        createRemoteMessages(SmsIntegrationUtils.Test10.conversation_1);
        startSynchro();

        createRemoteMessages(SmsIntegrationUtils.Test10.conversation_2);
        for (SmsDataObject sms : SmsIntegrationUtils.Test10.conversation_2) {
            mXmsLog.addSms(sms);
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                    sms.getContact()), Test9.imapReadStatus, Test9.imapDeleteStatus,
                    PushStatus.PUSHED, MessageType.SMS, sms.getMessageId(), null));
        }

        startSynchro();

        createRemoteMessages(SmsIntegrationUtils.Test10.conversation_3);
        startSynchro();

        assertEquals(SmsIntegrationUtils.Test10.conversation_1.length, mXmsLogEnvIntegration
                .getMessages(MimeType.TEXT_MESSAGE, SmsIntegrationUtils.Test10.contact1).size());
        assertEquals(SmsIntegrationUtils.Test10.conversation_2.length, mXmsLogEnvIntegration
                .getMessages(MimeType.TEXT_MESSAGE, SmsIntegrationUtils.Test10.contact2).size());
        assertEquals(SmsIntegrationUtils.Test10.conversation_3.length, mXmsLogEnvIntegration
                .getMessages(MimeType.TEXT_MESSAGE, SmsIntegrationUtils.Test10.contact3).size());

        assertEquals(SmsIntegrationUtils.Test10.conversation_1.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test10.folder1).size());
        assertEquals(SmsIntegrationUtils.Test10.conversation_2.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test10.folder2).size());
        assertEquals(SmsIntegrationUtils.Test10.conversation_3.length, mCmsLogTestIntegration
                .getMessages(SmsIntegrationUtils.Test10.folder3).size());
    }

    public void testLoad() throws FileAccessException, NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        for (int i = 0; i < TestLoad.iteration; i++) {
            createRemoteMessages(SmsIntegrationUtils.TestLoad.conversation_1);
            createRemoteMessages(SmsIntegrationUtils.Test10.conversation_2);
            for (SmsDataObject sms : SmsIntegrationUtils.Test10.conversation_2) {
                String msgId = IdGenerator.generateMessageID();
                mXmsLog.addSms(new SmsDataObject(msgId, sms.getContact(), sms.getBody(), sms
                        .getDirection(), sms.getReadStatus(), sms.getTimestamp(), sms
                        .getNativeProviderId(), sms.getNativeThreadId()));
                mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mSettings,
                        sms.getContact()),
                        sms.getReadStatus() == ReadStatus.READ ? CmsObject.ReadStatus.READ
                                : CmsObject.ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.SMS, msgId, sms.getNativeThreadId()));
            }
            createRemoteMessages(SmsIntegrationUtils.Test10.conversation_3);
        }

        startSynchro();

        Assert.assertEquals(
                TestLoad.iteration * SmsIntegrationUtils.TestLoad.conversation_1.length,
                mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                        SmsIntegrationUtils.TestLoad.contact1).size());
        Assert.assertEquals(
                TestLoad.iteration * SmsIntegrationUtils.TestLoad.conversation_2.length,
                mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                        SmsIntegrationUtils.TestLoad.contact2).size());
        Assert.assertEquals(
                TestLoad.iteration * SmsIntegrationUtils.TestLoad.conversation_3.length,
                mXmsLogEnvIntegration.getMessages(MimeType.TEXT_MESSAGE,
                        SmsIntegrationUtils.TestLoad.contact3).size());
    }

    private void deleteAllEntries() {
        mLocalContentResolver.delete(XmsData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(PartData.CONTENT_URI, null, null);
    }

    private void deleteLocalStorage(boolean deleteImapData, boolean deleteMessages) {
        if (deleteImapData) {
            mCmsLog.removeFolders(true);
            assertTrue(mCmsLog.getFolders().isEmpty());
            assertTrue(mCmsLogTestIntegration.getMessages().isEmpty());
        }
        if (deleteMessages) {
            deleteAllEntries();
        }
    }

    private void createRemoteMessages(XmsDataObject[] messages) throws NetworkException,
            PayloadException {
        CmsSyncPushMessageTask task = new CmsSyncPushMessageTask(mContext, mSettings, mXmsLog,
                mCmsLog);
        task.setBasicImapService(mBasicImapService);
        task.pushMessages(Arrays.asList(messages));
    }

    private void deleteRemoteStorage() throws NetworkException, PayloadException {
        CmsSyncDeleteTask deleteTask = new CmsSyncDeleteTask(Operation.DELETE_ALL, null, null);
        deleteTask.setBasicImapService(mBasicImapService);
        deleteTask.delete(null);
    }

    private void deleteRemoteMailbox(String mailbox) throws NetworkException, PayloadException,
            IOException, ImapException {
        CmsSyncDeleteTask deleteTask = new CmsSyncDeleteTask(Operation.DELETE_MAILBOX, mailbox,
                null);
        deleteTask.setBasicImapService(mBasicImapService);
        deleteTask.delete(mailbox);
        try {
            mBasicImapService.close();
        } catch (IOException ignore) {
        }
        mBasicImapService.init();
    }

    private void deleteRemoteMessages(String mailbox) throws NetworkException, PayloadException {
        CmsSyncDeleteTask deleteTask = new CmsSyncDeleteTask(Operation.DELETE_MESSAGES, mailbox,
                null);
        deleteTask.setBasicImapService(mBasicImapService);
        deleteTask.delete(mailbox);
    }

    private void updateRemoteFlags(List<FlagChange> changes) throws NetworkException,
            PayloadException {
        CmsSyncUpdateFlagTask task = new CmsSyncUpdateFlagTask(changes, null);
        task.setBasicImapService(mBasicImapService);
        task.updateFlags();
    }

    private void startSynchro() throws FileAccessException, NetworkException, PayloadException {
        mSyncStrategy.execute();
    }

}
