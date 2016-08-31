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

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.event.CmsEventHandler;
import com.gsma.rcs.core.cms.protocol.message.ImapImdnMessageImpl;
import com.gsma.rcs.core.cms.protocol.message.ImapOneToOneFileTransferImpl;
import com.gsma.rcs.core.cms.protocol.message.ImapOneToOneFileTransferImpl.FileTransferDescriptorTest;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.sync.process.BasicSyncStrategy;
import com.gsma.rcs.core.cms.sync.process.LocalStorage;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.FileTransferPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.smsmms.SmsMmsLog;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Philippe LEMORDANT
 */
public class SyncOneToOneFileTransferTest extends AndroidTestCase {

    private ImapServiceHandler mImapServiceHandler;
    private BasicSyncStrategy mSyncStrategy;
    private MessagingLog mMessagingLog;
    private CmsLog mCmsLog;
    private ImapCmsUtilTest mImapCmsUtilTest;
    private ImapOneToOneFileTransferImpl mOneToOneOutgoingTransfer;
    private ImapOneToOneFileTransferImpl mOneToOneIncomingTransfer;

    protected void setUp() throws Exception {
        super.setUp();
        RcsSettings mSettings = RcsSettingsMock.getMockSettings(mContext);
        AndroidFactory.setApplicationContext(mContext, mSettings);
        mCmsLog = CmsLog.getInstance(mContext);
        SmsMmsLog smsMmsLog = SmsMmsLog.getInstance(mContext, mContext.getContentResolver());
        LocalContentResolver localContentResolver = new LocalContentResolver(mContext);
        XmsLog xmsLog = XmsLog.getInstance(mContext, mSettings, localContentResolver);
        mMessagingLog = MessagingLog.getInstance(localContentResolver, mSettings);
        CmsSessionController cmsSessionCtrl = new CmsSessionController(mContext, null, mSettings,
                localContentResolver, xmsLog, mMessagingLog, mCmsLog, null);
        InstantMessagingService imService = new InstantMessagingService(null, mSettings, null,
                mMessagingLog, null, localContentResolver, mContext, null, cmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, mMessagingLog, null,
                mSettings, null, cmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, mMessagingLog, mSettings, null, mContext, cmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mContext, xmsLog, smsMmsLog);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mContext, cmsSessionCtrl, chatService,
                fileTransferService, imService, xmsLog, xmsManager, localContentResolver, smsMmsLog);
        CmsEventHandler cmsEventHandler = new CmsEventHandler(mContext, localContentResolver,
                mCmsLog, xmsLog, mMessagingLog, chatService, fileTransferService, cmsServiceImpl,
                imService, mSettings);
        LocalStorage localStorage = new LocalStorage(mSettings, mCmsLog, cmsEventHandler);
        mImapServiceHandler = new ImapServiceHandler(mSettings);
        BasicImapService imapService = mImapServiceHandler.openService();
        mSyncStrategy = new BasicSyncStrategy(mContext, mSettings, imapService, localStorage,
                xmsLog, mCmsLog);
        imapService.init();
        mImapCmsUtilTest = new ImapCmsUtilTest(mContext, mSettings, imapService, mCmsLog);

        long timestamp = 24L * 3600L * 1000L + 1001L;
        ContactId toContact = ContactUtil.createContactIdFromTrustedData("+330000001");
        ContactId fromContact = ContactUtil.createContactIdFromTrustedData("+330000002");
        FileTransferDescriptorTest fileDescriptor = new FileTransferDescriptorTest("filename.jpg",
                1234L, Uri.parse("http://www.gsma.com"), timestamp * 2);

        FileTransferDescriptorTest iconDescriptor = new FileTransferDescriptorTest("icon.jpg",
                4321L, Uri.parse("http://www.gsma.com.icon"), timestamp * 3);
        mOneToOneOutgoingTransfer = new ImapOneToOneFileTransferImpl(fromContact, toContact,
                RcsService.Direction.OUTGOING, timestamp, fileDescriptor, iconDescriptor, true,
                false);
        mOneToOneIncomingTransfer = new ImapOneToOneFileTransferImpl(fromContact, toContact,
                RcsService.Direction.INCOMING, timestamp, fileDescriptor, iconDescriptor, false,
                false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mImapCmsUtilTest.deleteRemoteStorage();
        mImapServiceHandler.closeService();
        mCmsLog.removeFolders(true);
        mMessagingLog.deleteAllEntries();
        RcsSettingsMock.restoreSettings();
    }

    private FileTransferHttpInfoDocument getInfoDocument(MessagingLog messagingLog, String ftId)
            throws FileAccessException {
        Cursor cursor = null;
        try {
            cursor = messagingLog.getFileTransferData(ftId);
            cursor.moveToNext();
            return messagingLog.getFileDownloadInfo(cursor);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public static class FileTransferHiddenField {

        public final FileTransferData.DownloadState mDownloadState;
        public final FileTransfer.ReasonCode mDownloadReason;

        public FileTransferHiddenField(MessagingLog messagingLog, String ftId) {
            Cursor cursor = null;
            try {
                cursor = messagingLog.getFileTransferData(ftId);
                cursor.moveToNext();
                int colDownloadStateIdx = cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_DOWNLOAD_STATE);
                mDownloadState = FileTransferData.DownloadState.valueOf(cursor
                        .getInt(colDownloadStateIdx));
                int colDownloadReasonIdx = cursor
                        .getColumnIndexOrThrow(FileTransferData.KEY_DOWNLOAD_REASON_CODE);
                mDownloadReason = FileTransfer.ReasonCode.valueOf(cursor
                        .getInt(colDownloadReasonIdx));

            } finally {
                CursorUtil.close(cursor);
            }
        }
    }

    public void testSyncNewOutgoingFileTransfer() throws Exception {
        // Create One to one file transfer on CMS
        String ftId = mOneToOneOutgoingTransfer.getMessageId();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mOneToOneOutgoingTransfer));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and file transfer providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(ftId);
        CmsRcsObject cmsObject = new CmsRcsObject(CmsData.MessageType.FILE_TRANSFER,
                mOneToOneOutgoingTransfer.getFolder(), ftId, CmsData.PushStatus.PUSHED,
                CmsData.ReadStatus.READ, CmsData.DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(mOneToOneOutgoingTransfer.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        FileTransferPersistedStorageAccessor accessor = new FileTransferPersistedStorageAccessor(
                ftId, mMessagingLog);
        assertEquals(RcsService.Direction.OUTGOING, accessor.getDirection());
        assertEquals(mOneToOneOutgoingTransfer.getRemote().toString(), accessor.getChatId());
        assertEquals(mOneToOneOutgoingTransfer.getRemote(), accessor.getRemoteContact());
        assertEquals(mOneToOneOutgoingTransfer.getMimeType(), accessor.getMimeType());
        long timestampInSecond = mOneToOneOutgoingTransfer.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(0L, accessor.getTimestampDelivered());
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertEquals(FileTransfer.State.TRANSFERRED, accessor.getState());
        assertFalse(accessor.isRead());
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED, accessor.getReasonCode());

        FileTransferDescriptorTest fileDesc = mOneToOneOutgoingTransfer.getFileDesc();
        assertEquals(fileDesc.mTimestampUntil / 1000L, accessor.getFileExpiration() / 1000L);
        assertEquals(fileDesc.mFilename, accessor.getFileName());
        assertEquals(fileDesc.mSize, accessor.getFileSize());

        FileTransferHttpInfoDocument infoDoc = getInfoDocument(mMessagingLog, ftId);
        assertEquals(fileDesc.mUrl, infoDoc.getUri());

        FileTransferHiddenField hiddenField = new FileTransferHiddenField(mMessagingLog, ftId);
        assertEquals(FileTransferData.DownloadState.QUEUED, hiddenField.mDownloadState);
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED, hiddenField.mDownloadReason);

        long timestampDelivered = 24L * 3600L * 1000L * 2L + 1001L;
        ImapImdnMessageImpl imdnMessage = new ImapImdnMessageImpl(ftId, null,
                mOneToOneOutgoingTransfer.getRemote(), mOneToOneOutgoingTransfer.getLocalContact(),
                RcsService.Direction.INCOMING, timestampDelivered,
                ImdnDocument.DeliveryStatus.DELIVERED, false, false);
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(imdnMessage));
        // Synchronize
        mSyncStrategy.execute();
        assertEquals(FileTransfer.State.DELIVERED, accessor.getState());
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
        assertEquals(timestampDelivered / 1000L, accessor.getTimestampDelivered() / 1000L);

        long timestampDisplayed = timestampDelivered * 2L;
        imdnMessage = new ImapImdnMessageImpl(ftId, null, mOneToOneOutgoingTransfer.getRemote(),
                mOneToOneOutgoingTransfer.getLocalContact(), RcsService.Direction.INCOMING,
                timestampDisplayed, ImdnDocument.DeliveryStatus.DISPLAYED, false, false);
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(imdnMessage));
        // Synchronize
        mSyncStrategy.execute();
        assertEquals(FileTransfer.State.DISPLAYED, accessor.getState());
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
        assertEquals(timestampDisplayed / 1000L, accessor.getTimestampDisplayed() / 1000L);
    }

    public void testSyncNewImdnDeliveredOugoingOneToOneFileTransfer() throws Exception {
        long timestampDeliver = 24L * 3600L * 1000L * 2L + 1001L;
        String ftId = mOneToOneOutgoingTransfer.getMessageId();
        ImapImdnMessageImpl imdnMessage = new ImapImdnMessageImpl(ftId, null,
                mOneToOneOutgoingTransfer.getRemote(), mOneToOneOutgoingTransfer.getLocalContact(),
                RcsService.Direction.INCOMING, timestampDeliver,
                ImdnDocument.DeliveryStatus.DELIVERED, false, false);
        // Create One to one file transfer message on CMS
        List<ImapCmsUtilTest.IImapRcsMessage> rcsMessages = new ArrayList<>();
        rcsMessages.add(mOneToOneOutgoingTransfer);
        rcsMessages.add(imdnMessage);
        mImapCmsUtilTest.createRemoteRcsMessages(rcsMessages);
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS log is correctly updated
        String imdnId = imdnMessage.getMessageId();
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(imdnId);
        CmsRcsObject cmsObject = new CmsRcsObject(CmsData.MessageType.IMDN,
                imdnMessage.getFolder(), imdnId, CmsData.PushStatus.PUSHED,
                CmsData.ReadStatus.UNREAD, CmsData.DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(imdnMessage.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        // Check that file transfer log is correctly updated
        FileTransferPersistedStorageAccessor accessor = new FileTransferPersistedStorageAccessor(
                ftId, mMessagingLog);
        long timestampInSecond = mOneToOneOutgoingTransfer.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(timestampDeliver / 1000L, accessor.getTimestampDelivered() / 1000L);
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertEquals(FileTransfer.State.DELIVERED, accessor.getState());
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
    }

    public void testSyncNewIncomingFileTransfer() throws Exception {
        // Create One to one file transfer message on CMS
        String ftId = mOneToOneIncomingTransfer.getMessageId();
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(mOneToOneIncomingTransfer));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and file transfer providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(ftId);
        CmsRcsObject cmsObject = new CmsRcsObject(CmsData.MessageType.FILE_TRANSFER,
                mOneToOneIncomingTransfer.getFolder(), ftId, CmsData.PushStatus.PUSHED,
                CmsData.ReadStatus.UNREAD, CmsData.DeleteStatus.NOT_DELETED, null);
        cmsObject.setUid(mOneToOneIncomingTransfer.getUid());
        assertEquals(cmsObject, cmsObjectFromDb);
        FileTransferPersistedStorageAccessor accessor = new FileTransferPersistedStorageAccessor(
                ftId, mMessagingLog);
        assertEquals(RcsService.Direction.INCOMING, accessor.getDirection());
        assertEquals(mOneToOneIncomingTransfer.getRemote().toString(), accessor.getChatId());
        assertEquals(mOneToOneIncomingTransfer.getRemote(), accessor.getRemoteContact());
        assertEquals(mOneToOneIncomingTransfer.getMimeType(), accessor.getMimeType());
        long timestampInSecond = mOneToOneIncomingTransfer.getTimestamp() / 1000L;
        assertEquals(timestampInSecond, accessor.getTimestamp() / 1000L);
        assertEquals(0L, accessor.getTimestampDelivered());
        assertEquals(timestampInSecond, accessor.getTimestampSent() / 1000L);
        assertEquals(FileTransfer.State.TRANSFERRED, accessor.getState());
        assertFalse(accessor.isRead());
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED, accessor.getReasonCode());

        FileTransferDescriptorTest fileDesc = mOneToOneIncomingTransfer.getFileDesc();
        assertEquals(fileDesc.mTimestampUntil / 1000L, accessor.getFileExpiration() / 1000L);
        assertEquals(fileDesc.mFilename, accessor.getFileName());
        assertEquals(fileDesc.mSize, accessor.getFileSize());

        FileTransferHttpInfoDocument infoDoc = getInfoDocument(mMessagingLog, ftId);
        assertEquals(fileDesc.mUrl, infoDoc.getUri());

        FileTransferHiddenField hiddenField = new FileTransferHiddenField(mMessagingLog, ftId);
        assertEquals(FileTransferData.DownloadState.QUEUED, hiddenField.mDownloadState);
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED, hiddenField.mDownloadReason);

        long timestampDisplayed = 24L * 3600L * 1000L * 2L + 1001L;
        ImapImdnMessageImpl imdnMessage = new ImapImdnMessageImpl(ftId, null,
                mOneToOneIncomingTransfer.getRemote(), mOneToOneIncomingTransfer.getLocalContact(),
                RcsService.Direction.OUTGOING, timestampDisplayed,
                ImdnDocument.DeliveryStatus.DISPLAYED, false, false);
        mImapCmsUtilTest.createRemoteRcsMessages(Collections
                .<ImapCmsUtilTest.IImapRcsMessage> singletonList(imdnMessage));
        // Synchronize
        mSyncStrategy.execute();
        assertEquals(FileTransfer.State.TRANSFERRED, accessor.getState());
        assertEquals(FileTransfer.ReasonCode.UNSPECIFIED, accessor.getReasonCode());
        assertEquals(timestampDisplayed / 1000L, accessor.getTimestampDisplayed() / 1000L);
    }

    public void testSyncSeenOneToOneFileTransfer() throws Exception {
        testSyncNewIncomingFileTransfer();
        String ftId = mOneToOneIncomingTransfer.getMessageId();
        String folder = mOneToOneIncomingTransfer.getFolder();
        // Mark the remote CMS message as seen
        CmsRcsObject cmsObject = new CmsRcsObject(CmsData.MessageType.FILE_TRANSFER, folder, ftId,
                mOneToOneIncomingTransfer.getUid(), CmsData.PushStatus.PUSHED,
                CmsData.ReadStatus.READ_REPORT_REQUESTED, CmsData.DeleteStatus.NOT_DELETED, null);
        mImapCmsUtilTest
                .updateRemoteFlags(folder, Collections.singletonList((CmsObject) cmsObject));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and file transfer providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(ftId);
        assertEquals(CmsData.DeleteStatus.NOT_DELETED, cmsObjectFromDb.getDeleteStatus());
        assertEquals(CmsData.ReadStatus.READ, cmsObjectFromDb.getReadStatus());
        assertTrue(mMessagingLog.isFileTransfer(ftId));
        FileTransferPersistedStorageAccessor accessor = new FileTransferPersistedStorageAccessor(
                ftId, mMessagingLog);
        assertTrue(accessor.isRead());
    }

    public void testSyncDeletedOneToOneFileTransfer() throws Exception {
        testSyncNewImdnDeliveredOugoingOneToOneFileTransfer();
        String ftId = mOneToOneOutgoingTransfer.getMessageId();
        String folder = mOneToOneOutgoingTransfer.getFolder();
        // Mark the remote CMS message as deleted
        CmsRcsObject cmsObject = new CmsRcsObject(CmsData.MessageType.FILE_TRANSFER, folder, ftId,
                mOneToOneOutgoingTransfer.getUid(), CmsData.PushStatus.PUSHED,
                CmsData.ReadStatus.READ, CmsData.DeleteStatus.DELETED_REPORT_REQUESTED, null);
        mImapCmsUtilTest
                .updateRemoteFlags(folder, Collections.singletonList((CmsObject) cmsObject));
        // Synchronize
        mSyncStrategy.execute();
        // Check that CMS and file transfer providers are correctly updated
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(ftId);
        assertEquals(CmsData.DeleteStatus.DELETED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mMessagingLog.isFileTransfer(ftId));
    }
}
