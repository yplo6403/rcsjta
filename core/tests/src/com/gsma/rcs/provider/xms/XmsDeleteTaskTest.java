/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.provider.xms;

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsData;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsRcsObject;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.FileUtilsTest;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Philippe LEMORDANT
 */
public class XmsDeleteTaskTest extends InstrumentationTestCase {
    private XmsLog mXmsLog;

    private LocalContentResolver mLocalContentResolver;
    private SmsDataObject mSms;
    private SmsDataObject mSms1;
    private SmsDataObject mSms2;
    private CmsServiceImpl mCmsServiceImpl;
    private CmsSessionController mCmsSessionCtrl;
    private CmsLog mCmsLog;
    private CmsObject mCmsObjectSms;
    private CmsObject mCmsObjectSms1;
    private CmsObject mCmsObjectSms2;
    private MmsDataObject mMms;
    private CmsObject mCmsObjectMms;

    protected void setUp() throws Exception {
        super.setUp();
        Context mContext = getInstrumentation().getContext();
        RcsSettings settings = RcsSettingsMock.getMockSettings(mContext);
        mLocalContentResolver = new LocalContentResolver(mContext.getContentResolver());
        mXmsLog = XmsLog.getInstance(mContext, settings, mLocalContentResolver);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));

        ContactId mContact1 = contactUtils.formatContact("+33000000001");
        String mFolder1 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact1);
        ContactId mContact2 = contactUtils.formatContact("+33000000002");
        String mFolder2 = com.gsma.rcs.core.cms.utils.CmsUtils.contactToCmsFolder(mContact2);

        long timestamp = 1;
        mSms = new SmsDataObject("sms-id", mContact2, "SMS test message",
                RcsService.Direction.INCOMING, ReadStatus.UNREAD, timestamp++, 200L, null);
        mCmsObjectSms = new CmsXmsObject(MessageType.SMS, mFolder2, mSms.getMessageId(), 1,
                PushStatus.PUSHED, CmsData.ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.NOT_DELETED, null);
        mSms1 = new SmsDataObject("sms-id1", mContact1, "SMS1 test message",
                RcsService.Direction.INCOMING, timestamp++, ReadStatus.UNREAD, "c'est vrai");
        mCmsObjectSms1 = new CmsXmsObject(MessageType.SMS, mFolder1, mSms1.getMessageId(), 2,
                PushStatus.PUSHED, CmsData.ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.NOT_DELETED, null);
        mSms2 = new SmsDataObject("sms-id2", mContact1, "SMS2 test message",
                RcsService.Direction.INCOMING, timestamp, ReadStatus.UNREAD, "c'est vrai");
        mCmsObjectSms2 = new CmsXmsObject(MessageType.SMS, mFolder1, mSms2.getMessageId(), 3,
                PushStatus.PUSHED, CmsData.ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.NOT_DELETED, null);

        File file = FileUtilsTest.createFileOnSdCard(mContext, "cat-test1.jpg");
        Uri mUriCat1 = Uri.fromFile(file);
        file = FileUtilsTest.createFileOnSdCard(mContext, "cat-test2.jpg");
        Uri mUriCat2 = Uri.fromFile(file);

        ArrayList<Uri> fileUris = new ArrayList<>();
        fileUris.add(mUriCat1);
        fileUris.add(mUriCat2);
        mMms = new MmsDataObject(mContext, "mms-id", mContact1, "MMS test subject",
                "MMS test message", RcsService.Direction.INCOMING, timestamp, fileUris, 100L,
                50000L);
        mCmsObjectMms = new CmsXmsObject(MessageType.MMS, mFolder1, mMms.getMessageId(), 1,
                PushStatus.PUSHED, CmsData.ReadStatus.READ_REPORT_REQUESTED,
                DeleteStatus.NOT_DELETED, null);

        mCmsLog = CmsLog.getInstance(mContext);
        MessagingLog mMessagingLog = MessagingLog.getInstance(mLocalContentResolver, settings);
        mCmsSessionCtrl = new CmsSessionController(mContext, null, null, settings,
                mLocalContentResolver, mXmsLog, mMessagingLog, mCmsLog);
        XmsManager xmsManager = new XmsManager(mContext, mContext.getContentResolver());
        InstantMessagingService imService = new InstantMessagingService(null, settings, null,
                mMessagingLog, null, mLocalContentResolver, mContext, null, mCmsSessionCtrl);
        ChatServiceImpl chatService = new ChatServiceImpl(imService, mMessagingLog, null, settings,
                null, mCmsSessionCtrl);
        FileTransferServiceImpl fileTransferService = new FileTransferServiceImpl(imService,
                chatService, mMessagingLog, settings, null, mContext, mCmsSessionCtrl);
        mCmsServiceImpl = new CmsServiceImpl(mContext, mCmsSessionCtrl, chatService,
                fileTransferService, imService, mXmsLog, settings, xmsManager,
                mLocalContentResolver);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mLocalContentResolver.delete(PartData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(XmsData.CONTENT_URI, null, null);
        mCmsLog.removeMessages();
        RcsSettingsMock.restoreSettings();
    }

    private void addMessageToCmsLog(CmsObject cmsObject) {
        if (CmsObject.isXmsData(cmsObject.getMessageType())) {
            mCmsLog.addXmsMessage((CmsXmsObject) cmsObject);
        } else {
            mCmsLog.addRcsMessage((CmsRcsObject) cmsObject);
        }
    }

    public void testXmsDeleteTaskOneSpecificSms_action() {
        mXmsLog.addSms(mSms);
        addMessageToCmsLog(mCmsObjectSms);
        assertTrue(mXmsLog.isMessagePersisted(mSms.getContact(), mSms.getMessageId()));
        CmsXmsObject cmsObjectFromDb = mCmsLog.getSmsData(mSms.getContact(), mSms.getMessageId());
        assertEquals(mCmsObjectSms, cmsObjectFromDb);
        XmsDeleteTask task = new XmsDeleteTask(mCmsServiceImpl, mLocalContentResolver,
                mSms.getContact(), mSms.getMessageId(), mCmsSessionCtrl);
        task.run();
        assertFalse(mXmsLog.isMessagePersisted(mSms.getContact(), mSms.getMessageId()));
        cmsObjectFromDb = mCmsLog.getSmsData(mSms.getContact(), mSms.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
    }

    public void testXmsDeleteTaskOneSpecificSms_event() {
        mXmsLog.addSms(mSms);
        addMessageToCmsLog(mCmsObjectSms);
        assertTrue(mXmsLog.isMessagePersisted(mSms.getContact(), mSms.getMessageId()));
        CmsXmsObject cmsObjectFromDb = mCmsLog.getSmsData(mSms.getContact(), mSms.getMessageId());
        assertEquals(mCmsObjectSms, cmsObjectFromDb);
        XmsDeleteTask task = new XmsDeleteTask(mCmsServiceImpl, mLocalContentResolver,
                mSms.getContact(), mSms.getMessageId(), mCmsLog);
        task.run();
        assertFalse(mXmsLog.isMessagePersisted(mSms.getContact(), mSms.getMessageId()));
        cmsObjectFromDb = mCmsLog.getSmsData(mSms.getContact(), mSms.getMessageId());
        assertEquals(DeleteStatus.DELETED, cmsObjectFromDb.getDeleteStatus());
    }

    public void testXmsDeleteTaskConversation() {
        mXmsLog.addSms(mSms1);
        addMessageToCmsLog(mCmsObjectSms1);
        CmsXmsObject cmsObjectFromDb = mCmsLog.getSmsData(mSms1.getContact(), mSms1.getMessageId());
        assertEquals(DeleteStatus.NOT_DELETED, cmsObjectFromDb.getDeleteStatus());
        mXmsLog.addSms(mSms2);
        addMessageToCmsLog(mCmsObjectSms2);
        cmsObjectFromDb = mCmsLog.getSmsData(mSms2.getContact(), mSms2.getMessageId());
        assertEquals(DeleteStatus.NOT_DELETED, cmsObjectFromDb.getDeleteStatus());
        XmsDeleteTask task = new XmsDeleteTask(mCmsServiceImpl, mLocalContentResolver,
                mSms1.getContact(), mCmsSessionCtrl);
        task.run();
        assertFalse(mXmsLog.isMessagePersisted(mSms1.getContact(), mSms1.getMessageId()));
        cmsObjectFromDb = mCmsLog.getSmsData(mSms1.getContact(), mSms1.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mXmsLog.isMessagePersisted(mSms2.getContact(), mSms2.getMessageId()));
        cmsObjectFromDb = mCmsLog.getSmsData(mSms2.getContact(), mSms2.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
    }

    public void testXmsDeleteTaskAll() {
        mXmsLog.addSms(mSms);
        addMessageToCmsLog(mCmsObjectSms);
        mXmsLog.addSms(mSms1);
        addMessageToCmsLog(mCmsObjectSms1);
        mXmsLog.addSms(mSms2);
        addMessageToCmsLog(mCmsObjectSms2);
        assertTrue(mXmsLog.isMessagePersisted(mSms.getContact(), mSms.getMessageId()));
        assertTrue(mXmsLog.isMessagePersisted(mSms1.getContact(), mSms1.getMessageId()));
        assertTrue(mXmsLog.isMessagePersisted(mSms2.getContact(), mSms2.getMessageId()));
        XmsDeleteTask task = new XmsDeleteTask(mCmsServiceImpl, mLocalContentResolver,
                mCmsSessionCtrl);
        task.run();
        assertFalse(mXmsLog.isMessagePersisted(mSms.getContact(), mSms.getMessageId()));
        CmsXmsObject cmsObjectFromDb = mCmsLog.getSmsData(mSms.getContact(), mSms.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mXmsLog.isMessagePersisted(mSms1.getContact(), mSms1.getMessageId()));
        cmsObjectFromDb = mCmsLog.getSmsData(mSms1.getContact(), mSms1.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
        assertFalse(mXmsLog.isMessagePersisted(mSms2.getContact(), mSms2.getMessageId()));
        cmsObjectFromDb = mCmsLog.getSmsData(mSms2.getContact(), mSms2.getMessageId());
        assertEquals(DeleteStatus.DELETED_REPORT_REQUESTED, cmsObjectFromDb.getDeleteStatus());
    }

    public void testMmsDeleteSpecific() {
        mXmsLog.addIncomingMms(mMms);
        addMessageToCmsLog(mCmsObjectMms);
        assertTrue(mXmsLog.isMessagePersisted(mMms.getContact(), mMms.getMessageId()));
        CmsXmsObject cmsObjectFromDb = mCmsLog.getMmsData(mMms.getContact(), mMms.getMessageId());
        assertEquals(mCmsObjectMms, cmsObjectFromDb);
        List<MmsDataObject.MmsPart> partsBeforeDelete = mXmsLog.getParts(mMms.getMessageId());
        assertEquals(partsBeforeDelete.size(), 3);

        XmsDeleteTask task = new XmsDeleteTask(mCmsServiceImpl, mLocalContentResolver,
                mMms.getContact(), mMms.getMessageId(), mCmsLog);
        task.run();
        assertFalse(mXmsLog.isMessagePersisted(mMms.getContact(), mMms.getMessageId()));
        cmsObjectFromDb = mCmsLog.getMmsData(mMms.getContact(), mMms.getMessageId());
        assertEquals(DeleteStatus.DELETED, cmsObjectFromDb.getDeleteStatus());
        List<MmsDataObject.MmsPart> partsAfterDelete = mXmsLog.getParts(mMms.getMessageId());
        assertTrue(partsAfterDelete.isEmpty());
    }
}
