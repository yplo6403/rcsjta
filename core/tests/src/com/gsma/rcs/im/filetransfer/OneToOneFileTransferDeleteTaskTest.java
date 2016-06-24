/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.im.filetransfer;

import static com.gsma.rcs.utils.FileUtilsTest.doesFileExist;
import static com.gsma.rcs.utils.FileUtilsTest.getFileContent;

import com.gsma.rcs.core.cms.event.XmsEventHandler;
import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsData.ReadStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.messaging.OneToOneFileTransferDeleteTask;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.FileUtilsTest;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.content.Context;
import android.net.Uri;
import android.test.InstrumentationTestCase;

/**
 * @author Philippe LEMORDANT
 */
public class OneToOneFileTransferDeleteTaskTest extends InstrumentationTestCase {

    private LocalContentResolver mLocalContentResolver;
    private CmsSessionController mCmsSessionCtrl;
    private CmsLog mCmsLog;
    private MessagingLog mMessagingLog;
    private InstantMessagingService mInstantMessagingService;
    private FileUtilsTest.FileTransferHolder mFt1;
    private CmsObject mCmsObjectFt1;
    private Context mCtx;
    private FileTransferServiceImpl mFileTransferService;
    private FileUtilsTest.FileTransferHolder mFt2;
    private CmsObject mCmsObjectFt2;
    private FileUtilsTest.FileTransferHolder mFt3;
    private CmsObject mCmsObjectFt3;

    protected void setUp() throws Exception {
        super.setUp();
        mCtx = getInstrumentation().getContext();
        RcsSettings settings = RcsSettingsMock.getMockSettings(mCtx);
        RcsSettingsMock.setInvalidCmsServer();
        assertNull(settings.getMessageStoreUri());
        mLocalContentResolver = new LocalContentResolver(mCtx.getContentResolver());
        XmsLog mXmsLog = XmsLog.getInstance(mCtx, settings, mLocalContentResolver);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mCtx));

        ContactId mContact1 = contactUtils.formatContact("+33000000001");
        String mFolder1 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact1);
        ContactId mContact2 = contactUtils.formatContact("+33000000002");
        String mFolder2 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact2);

        Uri mUriCat1 = Uri.fromFile(FileUtilsTest.createFileOnSdCard(mCtx, "cat-test1.jpg"));
        Uri mUriCat2 = Uri.fromFile(FileUtilsTest.createFileOnSdCard(mCtx, "cat-test2.jpg"));
        Uri mUriCat3 = Uri.fromFile(FileUtilsTest.createFileOnSdCard(mCtx, "cat-test3.jpg"));
        Uri mUriCat4 = Uri.fromFile(FileUtilsTest.createFileOnSdCard(mCtx, "cat-test4.jpg"));

        long timestamp = 1;
        mFt1 = new FileUtilsTest.FileTransferHolder(mContact1, "ft1", timestamp++, timestamp++,
                RcsService.Direction.OUTGOING, FileTransfer.State.TRANSFERRED,
                FileTransfer.ReasonCode.UNSPECIFIED, mUriCat1, 1L, mUriCat2);
        mCmsObjectFt1 = new CmsRcsObject(MessageType.FILE_TRANSFER, mFolder1, mFt1.mFtId, 1,
                PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null);
        mFt2 = new FileUtilsTest.FileTransferHolder(mContact1, "ft2", timestamp++, timestamp++,
                RcsService.Direction.OUTGOING, FileTransfer.State.TRANSFERRED,
                FileTransfer.ReasonCode.UNSPECIFIED, mUriCat3, 1L, null);
        mCmsObjectFt2 = new CmsRcsObject(MessageType.FILE_TRANSFER, mFolder1, mFt2.mFtId, 2,
                PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null);
        mFt3 = new FileUtilsTest.FileTransferHolder(mContact2, "ft3", timestamp++, timestamp,
                RcsService.Direction.OUTGOING, FileTransfer.State.TRANSFERRED,
                FileTransfer.ReasonCode.UNSPECIFIED, mUriCat4, 1L, null);
        mCmsObjectFt3 = new CmsRcsObject(MessageType.FILE_TRANSFER, mFolder2, mFt3.mFtId, 2,
                PushStatus.PUSHED, ReadStatus.READ_REPORT_REQUESTED, DeleteStatus.NOT_DELETED, null);
        mCmsLog = CmsLog.getInstance(mCtx);
        mMessagingLog = MessagingLog.getInstance(mLocalContentResolver, settings);
        mCmsSessionCtrl = new CmsSessionController(mCtx, null, null, settings,
                mLocalContentResolver, mXmsLog, mMessagingLog, mCmsLog);
        mInstantMessagingService = new InstantMessagingService(null, settings, null, mMessagingLog,
                null, mLocalContentResolver, mCtx, null, mCmsSessionCtrl);
        ChatServiceImpl mChatService = new ChatServiceImpl(mInstantMessagingService, mMessagingLog,
                null, settings, null, mCmsSessionCtrl);
        XmsManager xmsManager = new XmsManager(mCtx, mCtx.getContentResolver());
        mFileTransferService = new FileTransferServiceImpl(mInstantMessagingService, mChatService,
                mMessagingLog, settings, null, mCtx, mCmsSessionCtrl);
        CmsServiceImpl cmsServiceImpl = new CmsServiceImpl(mCtx, mCmsSessionCtrl, mChatService,
                mFileTransferService, mInstantMessagingService, mXmsLog, settings, xmsManager,
                mLocalContentResolver);
        mCmsSessionCtrl.register(cmsServiceImpl, mChatService, mFileTransferService,
                mInstantMessagingService);
        XmsEventHandler xmsEventHandler = new XmsEventHandler(mCmsLog, mXmsLog, settings,
                cmsServiceImpl);
        mCmsSessionCtrl.initialize(xmsEventHandler);
        mCmsSessionCtrl.start();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mMessagingLog.deleteAllEntries();
        mCmsLog.removeMessages();
        mCmsSessionCtrl.stop(ImsServiceSession.TerminationReason.TERMINATION_BY_SYSTEM);
        RcsSettingsMock.restoreSettings();
    }

    private void createFileTransfer(FileUtilsTest.FileTransferHolder holder) {
        MmContent mContentFile = getFileContent(mCtx, holder.mFile);
        MmContent mContentFileIcon = null;
        if (holder.mFileIcon != null) {
            mContentFileIcon = getFileContent(mCtx, holder.mFileIcon);
        }
        mMessagingLog.addOneToOneFileTransfer(holder.mFtId, holder.mContact, holder.mDir,
                mContentFile, mContentFileIcon, holder.mState, holder.mReason, holder.mTimestamp,
                holder.mTimestampSent, holder.mFileExpiration,
                holder.mFileIcon != null ? holder.mFileExpiration : 0L);
    }

    private void addMessageToCmsLog(CmsObject cmsObject) {
        if (CmsObject.isXmsData(cmsObject.getMessageType())) {
            mCmsLog.addXmsMessage((CmsXmsObject) cmsObject);
        } else {
            mCmsLog.addRcsMessage((CmsRcsObject) cmsObject);
        }
    }

    public void testOneToOneFileTransferDeleteTaskSpecific_action() {
        createFileTransfer(mFt1);
        assertTrue(doesFileExist(mFt1.mFile));
        assertTrue(doesFileExist(mFt1.mFileIcon));
        assertTrue(mMessagingLog.isFileTransfer(mFt1.mFtId));
        addMessageToCmsLog(mCmsObjectFt1);
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt1
                .getMessageId());
        assertEquals(mCmsObjectFt1, cmsObjectFromDb);

        OneToOneFileTransferDeleteTask task = new OneToOneFileTransferDeleteTask(
                mFileTransferService, mInstantMessagingService, mLocalContentResolver, mFt1.mFtId,
                mCmsSessionCtrl);
        task.run();

        assertFalse(mMessagingLog.isFileTransfer(mFt1.mFtId));
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt1.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(doesFileExist(mFt1.mFile));
        assertFalse(doesFileExist(mFt1.mFileIcon));
    }

    public void testOneToOneFileTransferDeleteTaskSpecific_event() {
        createFileTransfer(mFt1);
        assertTrue(doesFileExist(mFt1.mFile));
        assertTrue(doesFileExist(mFt1.mFileIcon));
        assertTrue(mMessagingLog.isFileTransfer(mFt1.mFtId));
        addMessageToCmsLog(mCmsObjectFt1);
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt1
                .getMessageId());
        assertEquals(mCmsObjectFt1, cmsObjectFromDb);

        OneToOneFileTransferDeleteTask task = new OneToOneFileTransferDeleteTask(
                mFileTransferService, mInstantMessagingService, mLocalContentResolver, mFt1.mFtId,
                mCmsLog);
        task.run();

        assertFalse(mMessagingLog.isFileTransfer(mFt1.mFtId));
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt1.getMessageId());
        assertEquals(DeleteStatus.DELETED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(doesFileExist(mFt1.mFile));
        assertFalse(doesFileExist(mFt1.mFileIcon));
    }

    public void testOneToOneChatMessageDeleteTaskConversation() {
        createFileTransfer(mFt1);
        assertTrue(mMessagingLog.isFileTransfer(mFt1.mFtId));
        addMessageToCmsLog(mCmsObjectFt1);
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt1
                .getMessageId());
        assertEquals(mCmsObjectFt1, cmsObjectFromDb);

        createFileTransfer(mFt2);
        assertTrue(mMessagingLog.isFileTransfer(mFt2.mFtId));
        assertTrue(doesFileExist(mFt2.mFile));
        addMessageToCmsLog(mCmsObjectFt2);
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt2.getMessageId());
        assertEquals(mCmsObjectFt2, cmsObjectFromDb);

        OneToOneFileTransferDeleteTask task = new OneToOneFileTransferDeleteTask(
                mFileTransferService, mInstantMessagingService, mLocalContentResolver,
                mFt1.mContact, mCmsSessionCtrl);
        task.run();

        assertFalse(mMessagingLog.isFileTransfer(mFt1.mFtId));
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt1.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(doesFileExist(mFt1.mFile));
        assertFalse(doesFileExist(mFt1.mFileIcon));

        assertFalse(mMessagingLog.isFileTransfer(mFt2.mFtId));
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt2.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(doesFileExist(mFt2.mFile));
    }

    public void testOneToOneChatMessageDeleteTaskAll() {
        createFileTransfer(mFt1);
        assertTrue(mMessagingLog.isFileTransfer(mFt1.mFtId));
        addMessageToCmsLog(mCmsObjectFt1);
        CmsRcsObject cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt1
                .getMessageId());
        assertEquals(mCmsObjectFt1, cmsObjectFromDb);

        createFileTransfer(mFt2);
        assertTrue(mMessagingLog.isFileTransfer(mFt2.mFtId));
        assertTrue(doesFileExist(mFt2.mFile));
        addMessageToCmsLog(mCmsObjectFt2);
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt2.getMessageId());
        assertEquals(mCmsObjectFt2, cmsObjectFromDb);

        createFileTransfer(mFt3);
        assertTrue(mMessagingLog.isFileTransfer(mFt3.mFtId));
        assertTrue(doesFileExist(mFt3.mFile));
        addMessageToCmsLog(mCmsObjectFt3);
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt3.getMessageId());
        assertEquals(mCmsObjectFt3, cmsObjectFromDb);

        OneToOneFileTransferDeleteTask task = new OneToOneFileTransferDeleteTask(
                mFileTransferService, mInstantMessagingService, mLocalContentResolver,
                mCmsSessionCtrl);
        task.run();

        assertFalse(mMessagingLog.isFileTransfer(mFt1.mFtId));
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt1.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(doesFileExist(mFt1.mFile));
        assertFalse(doesFileExist(mFt1.mFileIcon));

        assertFalse(mMessagingLog.isFileTransfer(mFt2.mFtId));
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt2.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(doesFileExist(mFt2.mFile));

        assertFalse(mMessagingLog.isFileTransfer(mFt3.mFtId));
        cmsObjectFromDb = mCmsLog.getChatOrImdnOrFileTransferData(mCmsObjectFt3.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(doesFileExist(mFt3.mFile));
    }

}
