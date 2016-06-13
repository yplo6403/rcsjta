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
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test1;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test2;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test5;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test7;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test8;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.Test9;
import com.gsma.rcs.core.cms.integration.MmsIntegrationUtils.TestLoad;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncDeleteTask;
import com.gsma.rcs.core.cms.sync.scheduler.task.CmsSyncDeleteTask.Operation;
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
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MmsTest extends AndroidTestCase {

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
        mSettings = RcsSettingsMock.getMockSettings(getContext());
        AndroidFactory.setApplicationContext(mContext, mSettings);
        mCmsLog = CmsLog.getInstance(mContext);
        mCmsLogTestIntegration = CmsLogTestIntegration.getInstance(mContext);
        mLocalContentResolver = new LocalContentResolver(mContext);
        mXmsLog = XmsLog.getInstance(mContext, mSettings, mLocalContentResolver);
        MessagingLog messagingLog = MessagingLog.getInstance(new LocalContentResolver(mContext),
                mSettings);
        mXmsLogEnvIntegration = XmsLogEnvIntegration.getInstance(mContext);
        CmsSessionController cmsSessionCtrl = new CmsSessionController(mContext, null, null, mSettings,
                mLocalContentResolver, mXmsLog, messagingLog, mCmsLog);
        InstantMessagingService imService = new InstantMessagingService(null, mSettings, null,
                messagingLog, null, mLocalContentResolver, mContext, null, cmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, messagingLog, null, mSettings,
                null, cmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, messagingLog, mSettings, null, mContext, cmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, mContext.getContentResolver());
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, cmsSessionCtrl, chatService,
                fileTransferService, imService, mXmsLog, mSettings, xmsManager,
                mLocalContentResolver);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, mLocalContentResolver,
                mCmsLog, mXmsLog, messagingLog, chatService, fileTransferService, cmsServiceImpl,
                imService, mSettings);
        LocalStorage localStorage = new LocalStorage(mSettings, mCmsLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(mSettings);
        mBasicImapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(mContext, mSettings, mBasicImapService, localStorage,
                mXmsLog, mCmsLog);
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
        createRemoteMessages(Test1.conversation);
        startSynchro();
        // check that messages are present in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        int nbFolders = mCmsLog.getFolders().size();
        assertTrue(nbFolders > 0);
        imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        assertEquals(Test1.conversation.length, imapData.size());
        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        assertEquals(Test1.conversation.length, messages.size());
        for (XmsDataObject message : messages) {
            Assert.assertEquals(Test1.readStatus, message.getReadStatus());
        }
        startSynchro();
        // check that messages are present in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        assertEquals(nbFolders, mCmsLog.getFolders().size());
        imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        assertEquals(Test1.conversation.length, imapData.size());
    }

    /**
     * Test2 Test 1 + Step 0 : mark conversation as seen on CMS
     * <ul>
     * <li>step 1 : start a sync : messages are marked as seen in local storage</li>
     * <li>Step 2 : mark conversation as deleted on CMS</li>
     * <li>step 3 : start a sync : messages are marked as deleted in local storage</li>
     * </ul>
     */
    public void test2() throws NetworkException, PayloadException, FileAccessException {
        test1();
        // update messages with 'seen' flag on CMS
        updateRemoteFlags(Test1.folderName, Arrays.asList(Test2.cmsObjectReadRequested));
        // sync with CMS
        startSynchro();
        // check that messages are marked as 'Seen' in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        int nbFolders = mCmsLog.getFolders().size();
        assertTrue(nbFolders > 0);
        assertEquals(Test1.conversation.length, mCmsLogTestIntegration
                .getMessages(Test1.folderName).size());
        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        assertEquals(Test1.conversation.length, messages.size());
        for (XmsDataObject message : messages) {
            Assert.assertEquals(ReadStatus.READ, message.getReadStatus());
        }
        // update messages with 'deleted' flag on CMS
        updateRemoteFlags(Test1.folderName, Arrays.asList(Test2.cmsObjectDeletedRequested));
        // sync with CMS
        startSynchro();
        // check that messages are marked as 'Deleted' in local storage
        assertFalse(mCmsLog.getFolders().isEmpty());
        assertEquals(nbFolders, mCmsLog.getFolders().size());
        assertEquals(Test1.conversation.length, mCmsLogTestIntegration
                .getMessages(Test1.folderName).size());
        messages = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
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
        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (XmsDataObject msg : messages) {
            mCmsLog.updateXmsReadStatus(Test1.contact, msg.getMessageId(),
                    CmsObject.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        // sync with CMS : during this first sync, messages are marked as 'Seen' on CMS
        startSynchro();
        String folder = CmsUtils.contactToCmsFolder(Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsObject.ReadStatus.READ, cmsObject.getReadStatus());
        }
        messages = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (XmsDataObject msg : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.MMS, msg.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
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
    public void test4() throws NetworkException, PayloadException, FileAccessException {
        test1();
        // delete mailbox on CMS
        try {
            deleteRemoteMailbox(CmsUtils.contactToCmsFolder(Test1.contact));
        } catch (Exception ignore) {
        }
        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateXmsReadStatus(Test1.contact, sms.getMessageId(),
                    CmsObject.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        startSynchro();
        String folder = CmsUtils.contactToCmsFolder(Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsObject.ReadStatus.READ_REPORT_REQUESTED,
                    cmsObject.getReadStatus());
        }
        for (XmsDataObject sms : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.MMS, sms.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
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
    public void test5() throws NetworkException, FileAccessException, PayloadException {
        test1();
        // mark messages as deleted on server and expunge them.
        updateRemoteFlags(Test1.folderName, Arrays.asList(Test5.cmsObjectDeletedRequested));
        deleteRemoteMessages(CmsUtils.contactToCmsFolder(Test1.contact));
        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (XmsDataObject sms : messages) {
            mCmsLog.updateXmsReadStatus(Test1.contact, sms.getMessageId(),
                    CmsObject.ReadStatus.READ_REPORT_REQUESTED, null);
        }
        startSynchro();
        String folder = CmsUtils.contactToCmsFolder(Test1.contact);
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(CmsObject.ReadStatus.READ, cmsObject.getReadStatus());
        }
        for (XmsDataObject sms : messages) {
            mCmsLog.updateRcsDeleteStatus(MessageType.MMS, sms.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED, null);
        }
        startSynchro();
        for (CmsObject cmsObject : mCmsLogTestIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, cmsObject.getDeleteStatus());
        }
    }

    /**
     * Test6 : check correlation algorithm (messages having the same mmsId)
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
        createRemoteMessages(Test1.conversation);
        // create messages in local storage
        for (MmsDataObject mms : Test1.conversation) {
            mXmsLog.addIncomingMms(mms);
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mms.getContact()),
                    CmsObject.ReadStatus.READ, DeleteStatus.NOT_DELETED, PushStatus.PUSHED,
                    MessageType.MMS, mms.getMessageId(), null));
        }
        startSynchro();
        assertEquals(Test1.conversation.length,
                mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, Test1.contact)
                        .size());
    }

    /**
     * Test6 : check correlation algorithm (messages having the same mmsId)
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
        createRemoteMessages(Test7.conversation);
        // create messages in local storage
        for (MmsDataObject mms : Test7.conversation) {
            mXmsLog.addIncomingMms(mms);
            String messageId = mms.getMessageId();
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mms.getContact()),
                    Test7.imapReadStatus, Test7.imapDeleteStatus, PushStatus.PUSHED,
                    MessageType.MMS, messageId, null));
        }
        startSynchro();
        List<XmsDataObject> mms = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Assert.assertEquals(Test7.conversation.length, mms.size());
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        Assert.assertEquals(Test7.conversation.length, imapData.size());
        Assert.assertEquals(imapData.get(4).getMessageId(), mms.get(0).getMessageId());
        Assert.assertEquals(imapData.get(3).getMessageId(), mms.get(1).getMessageId());
        Assert.assertEquals(imapData.get(2).getMessageId(), mms.get(2).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), mms.get(3).getMessageId());
    }

    /**
     * Test8 : check correlation algorithm (messages having the same content)<br>
     * The local storage has 3 messages (with the same content) whereas the CMS has only 2 messages.<br>
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
        createRemoteMessages(Test8.conversation_remote);
        // create messages in local storage
        for (MmsDataObject mms : Test8.conversation_local) {
            mXmsLog.addIncomingMms(mms);
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mms.getContact()),
                    Test8.imapReadStatus, Test8.imapDeleteStatus, PushStatus.PUSHED,
                    MessageType.MMS, mms.getMessageId(), null));
        }
        startSynchro();
        List<XmsDataObject> mms = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        Assert.assertEquals(Test8.conversation_local.length, mms.size());
        Assert.assertEquals(Test8.conversation_local.length, imapData.size());
        Assert.assertEquals(imapData.get(null).getMessageId(), "messageId3");
        Assert.assertEquals(imapData.get(2).getMessageId(), mms.get(1).getMessageId());
        Assert.assertEquals(imapData.get(1).getMessageId(), mms.get(2).getMessageId());
    }

    /**
     * Test9 : check correlation algorithm (messages having the mmsId)<br>
     * The local storage has 2 messages whereas the CMS has 2 messages (one is new)<br>
     * --> One local message will be mapped with a message from CMS<br>
     * --> One message should be downloaded from CMS (imap network trace)
     * <ul>
     * <li>step 1 : create a conversation on CMS server</li>
     * <li>step 2 : create a conversation in local storage</li>
     * <li>step 3 : start a sync</li>
     * </ul>
     */
    public void test9() throws FileAccessException, NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();
        // create messages on CMS
        createRemoteMessages(Test9.conversation_remote);
        // create messages in local storage
        for (MmsDataObject mms : Test9.conversation_local) {
            mXmsLog.addIncomingMms(mms);
            String messageId = mms.getMessageId();
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mms.getContact()),
                    Test9.imapReadStatus, Test9.imapDeleteStatus, PushStatus.PUSHED,
                    MessageType.MMS, messageId, null));
        }
        startSynchro();
        List<XmsDataObject> mms = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Map<Integer, CmsObject> imapData = mCmsLogTestIntegration.getMessages(Test1.folderName);
        Assert.assertEquals(2 + 1, mms.size());
        Assert.assertEquals(2 + 1, imapData.size());
        Assert.assertEquals(imapData.get(1).getMessageId(), mms.get(2).getMessageId());
        Assert.assertEquals(imapData.get(null).getMessageId(), "messageId2");
    }

    /**
     * Test10 : multi contacts
     */
    public void test10() throws NetworkException, FileAccessException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();
        createRemoteMessages(MmsIntegrationUtils.Test10.conversation_1);
        startSynchro();
        createRemoteMessages(MmsIntegrationUtils.Test10.conversation_2);
        for (MmsDataObject mms : MmsIntegrationUtils.Test10.conversation_2) {
            mXmsLog.addOutgoingMms(mms);
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mms.getContact()),
                    Test9.imapReadStatus, Test9.imapDeleteStatus, PushStatus.PUSHED,
                    MessageType.MMS, mms.getMessageId(), null));
        }
        startSynchro();
        createRemoteMessages(MmsIntegrationUtils.Test10.conversation_3);
        startSynchro();
        assertEquals(MmsIntegrationUtils.Test10.conversation_1.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test10.contact1)
                .size());
        assertEquals(MmsIntegrationUtils.Test10.conversation_2.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test10.contact2)
                .size());
        assertEquals(MmsIntegrationUtils.Test10.conversation_3.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test10.contact3)
                .size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_1.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test10.folder1).size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_2.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test10.folder2).size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_3.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test10.folder3).size());
    }

    /**
     * Test11 : multi contacts : mms with multiple recipients
     */
    public void test11() throws NetworkException, FileAccessException, PayloadException {
        String mmsId = MmsIntegrationUtils.Test11.conversation_1[0].getMessageId();
        deleteLocalStorage(true, true);
        deleteRemoteStorage();
        createRemoteMessages(MmsIntegrationUtils.Test11.conversation_1);
        createRemoteMessages(MmsIntegrationUtils.Test11.conversation_2);
        createRemoteMessages(MmsIntegrationUtils.Test11.conversation_3);
        startSynchro();
        assertEquals(MmsIntegrationUtils.Test11.conversation_1.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test11.contact1)
                .size());
        assertEquals(MmsIntegrationUtils.Test11.conversation_2.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test11.contact2)
                .size());
        assertEquals(MmsIntegrationUtils.Test11.conversation_3.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test11.contact3)
                .size());
        Assert.assertEquals(MmsIntegrationUtils.Test11.conversation_1.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test11.folder1).size());
        Assert.assertEquals(MmsIntegrationUtils.Test11.conversation_2.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test11.folder2).size());
        Assert.assertEquals(MmsIntegrationUtils.Test11.conversation_3.length,
                mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test11.folder3).size());
        // mark messages as deleted on server and expunge them.
        updateRemoteFlags(MmsIntegrationUtils.Test11.folder1,
                Arrays.asList(MmsIntegrationUtils.Test11.cmsObjectDeletedRequested));
        startSynchro();
        // check that conversation_1 message is marked as 'Deleted' in local storage
        assertEquals(3, mCmsLog.getFolders().size());
        Map<Integer, CmsObject> cmsMessages = mCmsLogTestIntegration
                .getMessages(MmsIntegrationUtils.Test11.folder1);
        assertEquals(MmsIntegrationUtils.Test11.conversation_1.length, cmsMessages.size());
        CmsObject cmsObject1 = cmsMessages.get(1);
        assertEquals(mmsId, cmsObject1.getMessageId());
        assertEquals(DeleteStatus.DELETED, cmsObject1.getDeleteStatus());
        // check that conversation_2 message is marked as 'Not Deleted' in local storage
        cmsMessages = mCmsLogTestIntegration.getMessages(MmsIntegrationUtils.Test11.folder2);
        assertEquals(MmsIntegrationUtils.Test11.conversation_2.length, cmsMessages.size());
        CmsObject cmsObject2 = cmsMessages.get(1);
        assertEquals(mmsId, cmsObject2.getMessageId());
        assertEquals(DeleteStatus.NOT_DELETED, cmsObject2.getDeleteStatus());
        // Check that message is deleted from xms
        List<XmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test11.contact1);
        assertTrue(messages.isEmpty());
        messages = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                MmsIntegrationUtils.Test11.contact2);
        assertEquals(1, messages.size());
        // check that parts are noy deleted
        assertTrue(mXmsLog.isPartPersisted(mmsId));
        // mark messages as deleted on server and expunge them.
        updateRemoteFlags(MmsIntegrationUtils.Test11.folder2,
                Arrays.asList(MmsIntegrationUtils.Test11.cmsObjectDeletedRequested));
        updateRemoteFlags(MmsIntegrationUtils.Test11.folder3,
                Arrays.asList(MmsIntegrationUtils.Test11.cmsObjectDeletedRequested));
        startSynchro();
        // check that parts are deleted
        assertFalse(mXmsLog.isPartPersisted(mmsId));
    }

    public void testLoad() throws FileAccessException, NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();
        createRemoteMessages(TestLoad.conversation_1);
        createRemoteMessages(TestLoad.conversation_2);
        for (MmsDataObject mms : TestLoad.conversation_2) {
            mXmsLog.addOutgoingMms(new MmsDataObject(mms.getMessageId(), mms.getContact(), mms
                    .getSubject(), mms.getDirection(), ReadStatus.UNREAD, mms.getTimestamp(), mms
                    .getNativeProviderId(), mms.getNativeThreadId(), mms.getMmsParts()));
            mCmsLog.addMessage(new CmsObject(CmsUtils.contactToCmsFolder(mms.getContact()),
                    CmsObject.ReadStatus.UNREAD, DeleteStatus.NOT_DELETED, PushStatus.PUSHED,
                    MessageType.MMS, mms.getMessageId(), mms.getNativeThreadId()));
        }
        createRemoteMessages(TestLoad.conversation_3);
        startSynchro();
        assertEquals(TestLoad.conversation_1.length,
                mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact1)
                        .size());
        assertEquals(TestLoad.conversation_2.length,
                mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact2)
                        .size());
        assertEquals(TestLoad.conversation_3.length,
                mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact3)
                        .size());
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
        CmsSyncPushMessageTask task = new CmsSyncPushMessageTask(mContext, mSettings, null, mXmsLog);
        task.pushMessages(mBasicImapService, Arrays.asList(messages));
    }

    private void deleteRemoteStorage() throws PayloadException, NetworkException {
        CmsSyncDeleteTask deleteTask = new CmsSyncDeleteTask(Operation.DELETE_ALL, null, null);
        deleteTask.delete(mBasicImapService, null);
    }

    private void deleteRemoteMailbox(String mailbox) throws NetworkException, PayloadException,
            IOException, ImapException {
        CmsSyncDeleteTask deleteTask = new CmsSyncDeleteTask(Operation.DELETE_MAILBOX, mailbox,
                null);
        deleteTask.delete(mBasicImapService, mailbox);
        try {
            mBasicImapService.close();
        } catch (IOException ignore) {
        }
        mBasicImapService.init();
    }

    private void deleteRemoteMessages(String mailbox) throws NetworkException, PayloadException {
        CmsSyncDeleteTask deleteTask = new CmsSyncDeleteTask(Operation.DELETE_MESSAGES, mailbox,
                null);
        deleteTask.delete(mBasicImapService, mailbox);
    }

    private void updateRemoteFlags(String remoteFolder, List<CmsObject> cmsObjects)
            throws NetworkException, PayloadException, FileAccessException {
        CmsSyncUpdateFlagTask task = new CmsSyncUpdateFlagTask(remoteFolder, cmsObjects, null);
        task.execute(mBasicImapService);
    }

    private void startSynchro() throws FileAccessException, NetworkException, PayloadException {
        mSyncStrategy.execute();
    }

}
