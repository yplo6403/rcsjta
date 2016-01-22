
package com.gsma.rcs.cms.integration;

import com.gsma.rcs.cms.event.CmsEventHandler;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.imap.task.DeleteTask;
import com.gsma.rcs.cms.imap.task.DeleteTask.Operation;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.integration.MmsIntegrationUtils.Test1;
import com.gsma.rcs.cms.integration.MmsIntegrationUtils.Test2;
import com.gsma.rcs.cms.integration.MmsIntegrationUtils.Test7;
import com.gsma.rcs.cms.integration.MmsIntegrationUtils.Test8;
import com.gsma.rcs.cms.integration.MmsIntegrationUtils.Test9;
import com.gsma.rcs.cms.integration.MmsIntegrationUtils.TestLoad;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.ImapLogEnvIntegration;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.xms.XmsLogEnvIntegration;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.sync.strategy.BasicSyncStrategy;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactUtil;

import com.sonymobile.rcs.imap.ImapException;

import android.content.Context;
import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MmsTest extends AndroidTestCase {

    private XmsLogEnvIntegration mXmsLogEnvIntegration;
    private RcsSettings mSettings;
    private ImapServiceController mImapServiceController;
    private BasicImapService mBasicImapService;
    private BasicSyncStrategy mSyncStrategy;
    private ImapLog mImapLog;
    private ImapLogEnvIntegration mImapLogEnvIntegration;
    private XmsLog mXmsLog;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContactUtil.getInstance(getContext());
        mSettings = RcsSettingsMock.getMockSettings(context);
        mImapLog = ImapLog.createInstance(context);
        mImapLogEnvIntegration = ImapLogEnvIntegration.getInstance(context);
        mXmsLog = XmsLog.createInstance(context, new LocalContentResolver(context));
        MessagingLog messagingLog = MessagingLog.createInstance(new LocalContentResolver(context),
                mSettings);
        mXmsLogEnvIntegration = XmsLogEnvIntegration.getInstance(context);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(context, mImapLog, mXmsLog,
                messagingLog, null, mSettings, null);
        LocalStorage localStorage = new LocalStorage(mImapLog, cmsEventHandler);
        mImapServiceController = new ImapServiceController(mSettings);
        mBasicImapService = mImapServiceController.createService();
        mSyncStrategy = new BasicSyncStrategy(context, mSettings, mImapServiceController,
                localStorage);
        mBasicImapService.init();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapServiceController.closeService();
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
    public void test1() throws FileAccessException, ImapServiceNotAvailableException,
            NetworkException, PayloadException {
        Map<Integer, MessageData> imapData;

        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(Test1.conversation);

        // start synchro
        startSynchro();

        // check that messages are present in local storage
        assertFalse(mImapLog.getFolders().isEmpty());
        int nbFolders = mImapLog.getFolders().size();
        assertTrue(nbFolders > 0);

        imapData = mImapLogEnvIntegration.getMessages(Test1.folderName);
        assertEquals(Test1.conversation.length, imapData.size());

        List<MmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        assertEquals(Test1.conversation.length, messages.size());
        for (MmsDataObject message : messages) {
            Assert.assertEquals(Test1.readStatus, message.getReadStatus());
        }

        // start synchro : all is up to date
        startSynchro();

        // check that messages are present in local storage
        assertFalse(mImapLog.getFolders().isEmpty());
        assertEquals(nbFolders, mImapLog.getFolders().size());

        imapData = mImapLogEnvIntegration.getMessages(Test1.folderName);
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
    public void test2() throws NetworkException, PayloadException,
            ImapServiceNotAvailableException, FileAccessException {
        test1();

        // update messages with 'seen' flag on CMS
        updateRemoteFlags(Arrays.asList(Test2.flagChangesSeen));

        // sync with CMS
        startSynchro();

        // check that messages are marked as 'Seen' in local storage
        assertFalse(mImapLog.getFolders().isEmpty());
        int nbFolders = mImapLog.getFolders().size();
        assertTrue(nbFolders > 0);
        assertEquals(Test1.conversation.length, mImapLogEnvIntegration
                .getMessages(Test1.folderName).size());
        List<MmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        assertEquals(Test1.conversation.length, messages.size());
        for (MmsDataObject message : messages) {
            Assert.assertEquals(ReadStatus.READ, message.getReadStatus());
        }

        // update messages with 'deleted' flag on CMS
        updateRemoteFlags(Arrays.asList(Test2.flagChangesDeleted));

        // sync with CMS
        startSynchro();

        // check that messages are marked as 'Deleted' in local storage
        assertFalse(mImapLog.getFolders().isEmpty());
        assertEquals(nbFolders, mImapLog.getFolders().size());
        assertEquals(Test1.conversation.length, mImapLogEnvIntegration
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
    public void test3() throws ImapServiceNotAvailableException, FileAccessException,
            NetworkException, PayloadException {
        test1();

        List<MmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (MmsDataObject msg : messages) {
            mImapLog.updateReadStatus(MessageType.MMS, msg.getMessageId(),
                    MessageData.ReadStatus.READ_REPORT_REQUESTED);
        }

        // sync with CMS : during this first sync, messages are marked as 'Seen' on CMS
        startSynchro();

        String folder = CmsUtils.contactToCmsFolder(mSettings, Test1.contact);
        for (MessageData messageData : mImapLogEnvIntegration.getMessages(folder).values()) {
            Assert.assertEquals(MessageData.ReadStatus.READ, messageData.getReadStatus());
        }

        messages = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (MmsDataObject msg : messages) {
            mImapLog.updateDeleteStatus(MessageType.MMS, msg.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED);
        }

        // sync with CMS : during this first sync, messages are marked as 'Deleted' on CMS
        startSynchro();

        for (MessageData messageData : mImapLogEnvIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, messageData.getDeleteStatus());
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
    public void test4() throws NetworkException, PayloadException, FileAccessException,
            ImapServiceNotAvailableException {

        test1();
        // delete mailbox on CMS
        try {
            deleteRemoteMailbox(CmsUtils.contactToCmsFolder(mSettings, Test1.contact));
        } catch (Exception e) {
        }

        List<MmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (MmsDataObject sms : messages) {
            mImapLog.updateReadStatus(MessageType.MMS, sms.getMessageId(),
                    MessageData.ReadStatus.READ_REPORT_REQUESTED);
        }

        startSynchro();

        String folder = CmsUtils.contactToCmsFolder(mSettings, Test1.contact);
        for (MessageData messageData : mImapLogEnvIntegration.getMessages(folder).values()) {
            Assert.assertEquals(MessageData.ReadStatus.READ_REPORT_REQUESTED,
                    messageData.getReadStatus());
        }

        for (MmsDataObject sms : messages) {
            mImapLog.updateDeleteStatus(MessageType.MMS, sms.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED);
        }

        startSynchro();
        for (MessageData messageData : mImapLogEnvIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED,
                    messageData.getDeleteStatus());
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
    public void test5() throws NetworkException, FileAccessException,
            ImapServiceNotAvailableException, PayloadException {
        test1();

        // mark messages as deleted on server and expunge them.
        updateRemoteFlags(Arrays.asList(MmsIntegrationUtils.Test5.flagChangesDeleted));
        deleteRemoteMessages(CmsUtils.contactToCmsFolder(mSettings, Test1.contact));

        List<MmsDataObject> messages = mXmsLogEnvIntegration.getMessages(
                MimeType.MULTIMEDIA_MESSAGE, Test1.contact);
        for (MmsDataObject sms : messages) {
            mImapLog.updateReadStatus(MessageType.MMS, sms.getMessageId(),
                    MessageData.ReadStatus.READ_REPORT_REQUESTED);
        }

        startSynchro();

        String folder = CmsUtils.contactToCmsFolder(mSettings, Test1.contact);
        for (MessageData messageData : mImapLogEnvIntegration.getMessages(folder).values()) {
            Assert.assertEquals(MessageData.ReadStatus.READ, messageData.getReadStatus());
        }

        for (MmsDataObject sms : messages) {
            mImapLog.updateDeleteStatus(MessageType.MMS, sms.getMessageId(),
                    DeleteStatus.DELETED_REPORT_REQUESTED);
        }

        startSynchro();

        for (MessageData messageData : mImapLogEnvIntegration.getMessages(folder).values()) {
            Assert.assertEquals(DeleteStatus.DELETED, messageData.getDeleteStatus());
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
    public void test6() throws FileAccessException, ImapServiceNotAvailableException,
            NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(Test1.conversation);

        // create messages in local storage
        for (MmsDataObject mms : Test1.conversation) {
            mXmsLog.addIncomingMms(mms);
            mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                    mms.getContact()), MessageData.ReadStatus.READ, DeleteStatus.NOT_DELETED,
                    PushStatus.PUSHED, MessageType.MMS, mms.getMessageId(), null));
        }

        startSynchro();

        Assert.assertEquals(Test1.conversation.length,
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
    public void test7() throws ImapServiceNotAvailableException, FileAccessException,
            NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(Test7.conversation);

        // create messages in local storage
        for (MmsDataObject mms : Test7.conversation) {
            mXmsLog.addIncomingMms(mms);
            String messageId = mms.getMessageId();
            mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                    mms.getContact()), Test7.imapReadStatus, Test7.imapDeleteStatus,
                    PushStatus.PUSHED, MessageType.MMS, messageId, null));
        }

        startSynchro();

        List<MmsDataObject> mms = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Assert.assertEquals(Test7.conversation.length, mms.size());
        Map<Integer, MessageData> imapData = mImapLogEnvIntegration.getMessages(Test1.folderName);
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
    public void test8() throws ImapServiceNotAvailableException, FileAccessException,
            NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(Test8.conversation_remote);

        // create messages in local storage
        for (MmsDataObject mms : Test8.conversation_local) {
            mXmsLog.addIncomingMms(mms);
            mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                    mms.getContact()), Test8.imapReadStatus, Test8.imapDeleteStatus,
                    PushStatus.PUSHED, MessageType.MMS, mms.getMessageId(), null));

        }

        startSynchro();

        List<MmsDataObject> mms = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Map<Integer, MessageData> imapData = mImapLogEnvIntegration.getMessages(Test1.folderName);

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
    public void test9() throws ImapServiceNotAvailableException, FileAccessException,
            NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        // create messages on CMS
        createRemoteMessages(Test9.conversation_remote);

        // create messages in local storage
        for (MmsDataObject mms : Test9.conversation_local) {
            mXmsLog.addIncomingMms(mms);
            String messageId = mms.getMessageId();
            mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                    mms.getContact()), Test9.imapReadStatus, Test9.imapDeleteStatus,
                    PushStatus.PUSHED, MessageType.MMS, messageId, null));
        }

        startSynchro();

        List<MmsDataObject> mms = mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE,
                Test1.contact);
        Map<Integer, MessageData> imapData = mImapLogEnvIntegration.getMessages(Test1.folderName);

        Assert.assertEquals(2 + 1, mms.size());
        Assert.assertEquals(2 + 1, imapData.size());
        Assert.assertEquals(imapData.get(1).getMessageId(), mms.get(2).getMessageId());
        Assert.assertEquals(imapData.get(null).getMessageId(), "messageId2");
    }

    /**
     * Test10 : multi contact
     */
    public void test10() throws NetworkException, ImapServiceNotAvailableException,
            FileAccessException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        createRemoteMessages(MmsIntegrationUtils.Test10.conversation_1);
        startSynchro();

        createRemoteMessages(MmsIntegrationUtils.Test10.conversation_2);
        for (MmsDataObject mms : MmsIntegrationUtils.Test10.conversation_2) {
            mXmsLog.addOutgoingMms(mms);
            mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                    mms.getContact()), Test9.imapReadStatus, Test9.imapDeleteStatus,
                    PushStatus.PUSHED, MessageType.MMS, mms.getMessageId(), null));
        }

        startSynchro();

        createRemoteMessages(MmsIntegrationUtils.Test10.conversation_3);
        startSynchro();

        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_1.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test10.contact1)
                .size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_2.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test10.contact2)
                .size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_3.length, mXmsLogEnvIntegration
                .getMessages(MimeType.MULTIMEDIA_MESSAGE, MmsIntegrationUtils.Test10.contact3)
                .size());

        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_1.length,
                mImapLogEnvIntegration.getMessages(MmsIntegrationUtils.Test10.folder1).size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_2.length,
                mImapLogEnvIntegration.getMessages(MmsIntegrationUtils.Test10.folder2).size());
        Assert.assertEquals(MmsIntegrationUtils.Test10.conversation_3.length,
                mImapLogEnvIntegration.getMessages(MmsIntegrationUtils.Test10.folder3).size());
    }

    public void testLoad() throws FileAccessException, ImapServiceNotAvailableException,
            NetworkException, PayloadException {
        deleteLocalStorage(true, true);
        deleteRemoteStorage();

        for (int i = 0; i < TestLoad.iteration; i++) {
            createRemoteMessages(TestLoad.conversation_1);
            createRemoteMessages(MmsIntegrationUtils.Test10.conversation_2);
            for (MmsDataObject mms : MmsIntegrationUtils.Test10.conversation_2) {
                String msgId = IdGenerator.generateMessageID();
                mXmsLog.addOutgoingMms(new MmsDataObject(mms.getMmsId(), msgId, mms.getContact(), mms
                        .getSubject(), mms.getDirection(), mms.getReadStatus(), mms.getTimestamp(),
                        mms.getNativeProviderId(), mms.getNativeThreadId(), mms.getMmsParts()));
                mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mSettings,
                        mms.getContact()),
                        mms.getReadStatus() == ReadStatus.READ ? MessageData.ReadStatus.READ
                                : MessageData.ReadStatus.UNREAD, DeleteStatus.NOT_DELETED,
                        PushStatus.PUSHED, MessageType.MMS, msgId, mms.getNativeThreadId()));
            }
            createRemoteMessages(MmsIntegrationUtils.Test10.conversation_3);
        }

        startSynchro();

        Assert.assertEquals(TestLoad.iteration * TestLoad.conversation_1.length,
                mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact1)
                        .size());
        Assert.assertEquals(TestLoad.iteration * TestLoad.conversation_2.length,
                mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact2)
                        .size());
        Assert.assertEquals(TestLoad.iteration * TestLoad.conversation_3.length,
                mXmsLogEnvIntegration.getMessages(MimeType.MULTIMEDIA_MESSAGE, TestLoad.contact3)
                        .size());
    }

    private void deleteLocalStorage(boolean deleteImapData, boolean deleteMessages) {
        if (deleteImapData) {
            mImapLog.removeFolders(true);
            assertTrue(mImapLog.getFolders().isEmpty());
            assertTrue(mImapLogEnvIntegration.getMessages().isEmpty());
        }
        if (deleteMessages) {
            mXmsLog.deleteAllEntries();
        }

    }

    private void createRemoteMessages(XmsDataObject[] messages)
            throws ImapServiceNotAvailableException {
        PushMessageTask task = new PushMessageTask(mContext, mSettings, mImapServiceController,
                mXmsLog, mImapLog);
        task.pushMessages(Arrays.asList(messages));
    }

    private void deleteRemoteStorage() throws ImapServiceNotAvailableException, PayloadException,
            NetworkException {
        DeleteTask deleteTask = new DeleteTask(mImapServiceController, Operation.DELETE_ALL, null,
                null);
        deleteTask.delete(null);
    }

    private void deleteRemoteMailbox(String mailbox) throws ImapServiceNotAvailableException,
            NetworkException, PayloadException, IOException, ImapException {
        DeleteTask deleteTask = new DeleteTask(mImapServiceController, Operation.DELETE_MAILBOX,
                mailbox, null);
        deleteTask.delete(mailbox);
        try {
            mBasicImapService.close();
        } catch (Exception e) {
        }
        mBasicImapService.init();
    }

    private void deleteRemoteMessages(String mailbox) throws ImapServiceNotAvailableException,
            NetworkException, PayloadException {
        DeleteTask deleteTask = new DeleteTask(mImapServiceController, Operation.DELETE_MESSAGES,
                mailbox, null);
        deleteTask.delete(mailbox);
    }

    private void updateRemoteFlags(List<FlagChange> changes)
            throws ImapServiceNotAvailableException, NetworkException, PayloadException {
        UpdateFlagTask task = new UpdateFlagTask(mImapServiceController, changes, null);
        task.updateFlags();
    }

    private void startSynchro() throws FileAccessException, ImapServiceNotAvailableException,
            NetworkException, PayloadException {
        mSyncStrategy.execute();
    }

}
