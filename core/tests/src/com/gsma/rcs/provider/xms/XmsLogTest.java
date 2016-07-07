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

package com.gsma.rcs.provider.xms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.core.cms.xms.mms.MmsFileSizeException;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.FileUtilsTest;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Philippe LEMORDANT
 */
public class XmsLogTest extends InstrumentationTestCase {
    private XmsLog mXmsLog;
    private String mFileName1;
    private String mFileName2;
    private Long mFileSize1;
    private Long mFileSize2;

    private LocalContentResolver mLocalContentResolver;
    private MmsDataObject mMms;
    private SmsDataObject mSms;
    private MmsDataObject mMms1;
    private MmsDataObject mMms2;
    private SmsDataObject mSms1;
    private SmsDataObject mSms2;
    private ContactId mContact1;

    protected void setUp() throws Exception {
        super.setUp();
        Context mContext = getInstrumentation().getContext();
        RcsSettings settings = RcsSettingsMock.getMockSettings(mContext);
        mLocalContentResolver = new LocalContentResolver(mContext.getContentResolver());
        mXmsLog = XmsLog.getInstance(mContext, settings, mLocalContentResolver);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mContact1 = contactUtils.formatContact("+33000000001");
        ContactId mContact2 = contactUtils.formatContact("+33000000002");

        mFileName1 = "cat-test1.jpg";
        File file = FileUtilsTest.createFileOnSdCard(mContext, mFileName1);
        mFileSize1 = file.length();
        Uri mUriCat1 = Uri.fromFile(file);

        mFileName2 = "cat-test2.jpg";
        file = FileUtilsTest.createFileOnSdCard(mContext, mFileName2);
        mFileSize2 = file.length();
        Uri mUriCat2 = Uri.fromFile(file);

        ArrayList<Uri> fileUris = new ArrayList<>();
        fileUris.add(mUriCat1);
        fileUris.add(mUriCat2);
        long timestamp = NtpTrustedTime.currentTimeMillis();
        mMms = new MmsDataObject(mContext, "mms-id", mContact1, "MMS test subject",
                "MMS test message", RcsService.Direction.INCOMING, timestamp++, fileUris, 100L,
                50000L);
        mMms1 = new MmsDataObject(mContext, "mms-id1", mContact1, "MMS1 test subject",
                "MMS1 test message", RcsService.Direction.OUTGOING, timestamp++, fileUris, 101L,
                50000L);
        mMms2 = new MmsDataObject(mContext, "mms-id1", mContact2, "MMS2 test subject",
                "MMS2 test message", RcsService.Direction.OUTGOING, timestamp++, fileUris, 102L,
                50000L);
        mSms = new SmsDataObject("sms-id", mContact2, "SMS test message",
                RcsService.Direction.INCOMING, ReadStatus.UNREAD, timestamp++, 200L, null);
        mSms1 = new SmsDataObject("sms-id1", mContact1, "SMS1 test message",
                RcsService.Direction.INCOMING, timestamp++, ReadStatus.UNREAD, "c'est vrai");
        mSms2 = new SmsDataObject("sms-id2", mContact1, "SMS2 test message",
                RcsService.Direction.INCOMING, timestamp, ReadStatus.UNREAD, "c'est vrai");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mLocalContentResolver.delete(PartData.CONTENT_URI, null, null);
        mLocalContentResolver.delete(XmsData.CONTENT_URI, null, null);
        RcsSettingsMock.restoreSettings();
    }

    public void testSmsMessage() {
        mXmsLog.addSms(mSms);
        assertTrue(mXmsLog.isMessagePersisted(mSms.getContact(), mSms.getMessageId()));
        Cursor cursor = mXmsLog.getXmsMessage(mSms.getContact(), mSms.getMessageId());
        assertEquals(cursor.getCount(), 1);
        assertTrue(cursor.moveToNext());
        String id = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
        assertEquals(mSms.getMessageId(), id);
        String contact = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT));
        assertEquals(mSms.getContact().toString(), contact);
        RcsService.Direction direction = RcsService.Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(XmsMessageLog.DIRECTION)));
        assertEquals(mSms.getDirection(), direction);
        String body = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.CONTENT));
        assertNotNull(body);
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MIME_TYPE));
        assertEquals(XmsMessageLog.MimeType.TEXT_MESSAGE, mimeType);
        long readTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(XmsMessageLog.TIMESTAMP));
        assertEquals(mSms.getTimestamp(), readTimestamp);
        long state = cursor.getLong(cursor.getColumnIndexOrThrow(XmsMessageLog.STATE));
        assertEquals(XmsMessage.State.QUEUED.toInt(), state);
        long reason = cursor.getLong(cursor.getColumnIndexOrThrow(XmsMessageLog.REASON_CODE));
        assertEquals(XmsMessage.ReasonCode.UNSPECIFIED.toInt(), reason);
        long readStatus = cursor.getLong(cursor.getColumnIndexOrThrow(XmsMessageLog.READ_STATUS));
        assertEquals(RcsService.ReadStatus.UNREAD.toInt(), readStatus);
        cursor.close();
    }

    public void testMmsMessage() throws IOException, RemoteException,
            OperationApplicationException, MmsFileSizeException, FileAccessException {
        mXmsLog.addIncomingMms(mMms);
        assertTrue(mXmsLog.isMessagePersisted(mMms.getContact(), mMms.getMessageId()));
        assertTrue(mXmsLog.isPartPersisted(mMms.getMessageId()));
        Cursor cursor = mXmsLog.getXmsMessage(mMms.getContact(), mMms.getMessageId());
        assertEquals(cursor.getCount(), 1);
        assertTrue(cursor.moveToNext());
        String id = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MESSAGE_ID));
        assertEquals(mMms.getMessageId(), id);
        String contact = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT));
        assertEquals(mMms.getContact().toString(), contact);
        RcsService.Direction direction = RcsService.Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(XmsMessageLog.DIRECTION)));
        assertEquals(mMms.getDirection(), direction);
        String body = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.CONTENT));
        assertNotNull(body);
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.MIME_TYPE));
        assertEquals(XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, mimeType);
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(XmsMessageLog.TIMESTAMP));
        assertEquals(mMms.getTimestamp(), timestamp);
        int state = cursor.getInt(cursor.getColumnIndexOrThrow(XmsMessageLog.STATE));
        assertEquals(XmsMessage.State.QUEUED.toInt(), state);
        int reason = cursor.getInt(cursor.getColumnIndexOrThrow(XmsMessageLog.REASON_CODE));
        assertEquals(XmsMessage.ReasonCode.UNSPECIFIED.toInt(), reason);
        int readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(XmsMessageLog.READ_STATUS));
        assertEquals(mMms.getReadStatus().toInt(), readStatus);
        cursor.close();
        /* test parts */
        List<MmsDataObject.MmsPart> parts = mXmsLog.getParts(mMms.getMessageId());
        assertEquals(parts.size(), 3);
        for (MmsPart part : parts) {
            assertEquals(mMms.getMessageId(), part.getMessageId());
            mimeType = part.getMimeType();
            if (XmsMessageLog.MimeType.TEXT_MESSAGE.equals(mimeType)) {
                assertEquals("MMS test message", part.getContentText());
                assertEquals(null, part.getFileName());
                assertEquals(null, part.getFileIcon());
                assertEquals(null, part.getFileSize());
            } else {
                assertEquals(null, part.getContentText());
                assertNotNull(part.getFileIcon());
                String fileName = part.getFileName();
                if (fileName.equals(mFileName1)) {
                    assertEquals(mFileSize1, part.getFileSize());

                } else if (part.getFileName().equals(mFileName2)) {
                    assertEquals(mFileSize2, part.getFileSize());

                } else {
                    fail("Unexpected filename : " + fileName);
                }
            }
        }
    }

    private boolean updateThreadId(ContactId contact, String messageId, Long threadId) {
        ContentValues values = new ContentValues();
        values.put(XmsData.KEY_NATIVE_THREAD_ID, threadId);
        return mLocalContentResolver.update(XmsData.CONTENT_URI, values,
                XmsLog.SEL_XMS_CONTACT_MSGID, new String[] {
                        contact.toString(), messageId
                }) > 0;
    }

    public void testGetMmsMessages() {
        mXmsLog.addIncomingMms(mMms);
        mXmsLog.addIncomingMms(mMms1);
        Map<ContactId, Set<String>> mmss = mXmsLog.getMmsMessages(300L);
        assertEquals(0, mmss.size());
        assertTrue(updateThreadId(mMms.getContact(), mMms.getMessageId(), 300L));
        assertTrue(updateThreadId(mMms.getContact(), mMms1.getMessageId(), 300L));
        mmss = mXmsLog.getMmsMessages(300L);
        assertEquals(1, mmss.size());
        Map.Entry<ContactId, Set<String>> entry = mmss.entrySet().iterator().next();
        ContactId contact = entry.getKey();
        assertEquals(mMms.getContact(), contact);
        Set<String> mmsIds = entry.getValue();
        assertEquals(2, mmsIds.size());
        assertTrue(mmsIds.contains(mMms.getMessageId()));
        assertTrue(mmsIds.contains(mMms1.getMessageId()));
    }

    public RcsService.ReadStatus getReadStatus(ContactId contact, String xmsId) {
        Cursor cursor = mXmsLog.getXmsMessage(contact, xmsId);
        assertEquals(cursor.getCount(), 1);
        assertTrue(cursor.moveToNext());
        int readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(XmsMessageLog.READ_STATUS));
        cursor.close();
        return RcsService.ReadStatus.valueOf(readStatus);
    }

    public void testMarkMessageAsRead() {
        mXmsLog.addSms(mSms);
        assertTrue(mXmsLog.markMessageAsRead(mSms.getContact(), mSms.getMessageId()));
        assertEquals(ReadStatus.READ, getReadStatus(mSms.getContact(), mSms.getMessageId()));
    }

    public void testGetUnreadMms() {
        mXmsLog.addIncomingMms(mMms);
        assertEquals(ReadStatus.UNREAD, getReadStatus(mMms.getContact(), mMms.getMessageId()));
        Map<ContactId, Set<String>> mmss = mXmsLog.getUnreadMms(300L);
        assertTrue(mmss.isEmpty());
        assertTrue(updateThreadId(mMms.getContact(), mMms.getMessageId(), 300L));
        mmss = mXmsLog.getUnreadMms(300L);
        assertEquals(1, mmss.size());
        Map.Entry<ContactId, Set<String>> entry = mmss.entrySet().iterator().next();
        ContactId contact = entry.getKey();
        assertEquals(mMms.getContact(), contact);
        Set<String> mmsIds = entry.getValue();
        assertEquals(1, mmsIds.size());
        assertEquals(mMms.getMessageId(), mmsIds.toArray()[0]);
    }

    public void testGetContactsForXmsId() {
        mXmsLog.addIncomingMms(mMms1);
        mXmsLog.addIncomingMms(mMms2);
        Set<ContactId> contacts = mXmsLog.getContactsForXmsId(mMms1.getMessageId());
        assertEquals(2, contacts.size());
        assertTrue(contacts.contains(mMms1.getContact()));
        assertTrue(contacts.contains(mMms2.getContact()));
        assertEquals(2, mXmsLog.getCountXms(mMms1.getMessageId()));
    }

    public void testGetCountXms() {
        mXmsLog.addIncomingMms(mMms1);
        assertEquals(1, mXmsLog.getCountXms(mMms1.getMessageId()));
        mXmsLog.addIncomingMms(mMms2);
        assertEquals(2, mXmsLog.getCountXms(mMms1.getMessageId()));
    }

    public void testGetMessageIdsMatchingCorrelator() {
        mXmsLog.addSms(mSms1);
        mXmsLog.addSms(mSms2);
        List<String> smsIds = mXmsLog.getMessageIdsMatchingCorrelator(mSms1.getContact(),
                mSms1.getDirection(), mSms1.getCorrelator());
        assertEquals(2, smsIds.size());
        // Order is important here
        assertEquals(mSms1.getMessageId(), smsIds.toArray()[1]);
        assertEquals(mSms2.getMessageId(), smsIds.toArray()[0]);
    }

    public void testUpdateSmsMessageId() {
        mXmsLog.addSms(mSms1);
        assertTrue(mXmsLog.updateSmsMessageId(mSms1.getContact(), mSms1.getMessageId(),
                "new-sms1-id"));
        assertTrue(mXmsLog.isMessagePersisted(mSms1.getContact(), "new-sms1-id"));
    }

    public void testDeleteMmsParts() throws FileAccessException {
        mXmsLog.addOutgoingMms(mMms2);
        List<MmsDataObject.MmsPart> partsBeforeDelete = mXmsLog.getParts(mMms2.getMessageId());
        assertEquals(partsBeforeDelete.size(), 3);
        for (MmsDataObject.MmsPart part : partsBeforeDelete) {
            Uri fileUri = part.getFile();
            if (fileUri != null) {
                File file = new File(fileUri.getPath());
                assertTrue(file.exists());
            }
        }
        mXmsLog.deleteMmsParts(mMms2.getMessageId(), mMms2.getDirection());
        List<MmsDataObject.MmsPart> partsAfterDelete = mXmsLog.getParts(mMms2.getMessageId());
        assertTrue(partsAfterDelete.isEmpty());
        for (MmsDataObject.MmsPart part : partsBeforeDelete) {
            Uri fileUri = part.getFile();
            if (fileUri != null) {
                File file = new File(fileUri.getPath());
                assertFalse(file.exists());
            }
        }
    }

    public void testSetStateAndReasonCode() {
        mXmsLog.addIncomingMms(mMms);
        XmsMessage.ReasonCode reasonCode = mXmsLog.getReasonCode(mMms.getContact(),
                mMms.getMessageId());
        assertEquals(XmsMessage.ReasonCode.UNSPECIFIED, reasonCode);
        XmsMessage.State state = mXmsLog.getState(mMms.getContact(), mMms.getMessageId());
        assertEquals(XmsMessage.State.QUEUED, state);
        assertTrue(mXmsLog.setStateAndReasonCode(mMms.getContact(), mMms.getMessageId(),
                XmsMessage.State.FAILED, XmsMessage.ReasonCode.FAILED_ERROR_RADIO_OFF));
        reasonCode = mXmsLog.getReasonCode(mMms.getContact(), mMms.getMessageId());
        assertEquals(XmsMessage.ReasonCode.FAILED_ERROR_RADIO_OFF, reasonCode);
        state = mXmsLog.getState(mMms.getContact(), mMms.getMessageId());
        assertEquals(XmsMessage.State.FAILED, state);
        mXmsLog.updateState(mMms.getContact(), mMms.getMessageId(), XmsMessage.State.DISPLAYED);
        state = mXmsLog.getState(mMms.getContact(), mMms.getMessageId());
        assertEquals(XmsMessage.State.DISPLAYED, state);
    }

    public void testSetStateAndTimestamp() {
        mXmsLog.addIncomingMms(mMms);
        assertTrue(mXmsLog.setStateAndTimestamp(mMms.getContact(), mMms.getMessageId(),
                XmsMessage.State.FAILED, XmsMessage.ReasonCode.FAILED_ERROR_RADIO_OFF, 123456789L,
                98764321L));
        XmsMessage.ReasonCode reasonCode = mXmsLog.getReasonCode(mMms.getContact(),
                mMms.getMessageId());
        assertEquals(XmsMessage.ReasonCode.FAILED_ERROR_RADIO_OFF, reasonCode);
        XmsMessage.State state = mXmsLog.getState(mMms.getContact(), mMms.getMessageId());
        assertEquals(XmsMessage.State.FAILED, state);
        Cursor cursor = mXmsLog.getXmsMessage(mMms.getContact(), mMms.getMessageId());
        assertEquals(cursor.getCount(), 1);
        assertTrue(cursor.moveToNext());
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(XmsMessageLog.TIMESTAMP));
        assertEquals(123456789L, timestamp);
        long timestampSent = cursor.getLong(cursor
                .getColumnIndexOrThrow(XmsMessageLog.TIMESTAMP_SENT));
        assertEquals(98764321L, timestampSent);
        cursor.close();
    }

    public void testGetSmsMessage() {
        SmsDataObject sms = new SmsDataObject("messageId", mContact1, "testGetSmsMessage",
                RcsService.Direction.INCOMING, ReadStatus.READ, 1234L, 56789L, 123456789L);
        mXmsLog.addSms(sms);
        Cursor cursor = mXmsLog.getSmsMessage(56789L);
        assertTrue(cursor.moveToNext());
        cursor.close();
    }

    public void testGetXmsDataObject() {
        mXmsLog.addIncomingMms(mMms);
        XmsDataObject xms = mXmsLog.getXmsDataObject(mMms.getContact(), mMms.getMessageId());
        assertTrue(xms instanceof MmsDataObject);
        MmsDataObject mms = (MmsDataObject) xms;
        assertEquals(mMms, mms);
    }
}
