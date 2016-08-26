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

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.xms.mms.MmsFileSizeException;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.FileUtilsTest;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH = 640;
    public static final int MMS_PARTS_MAX_FILE_SIZE = 200000; /* International limit */

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
        mMms = getMmsDataObject(mContext, "mms-id", mContact1, "MMS test subject",
                "MMS test message", RcsService.Direction.INCOMING, timestamp++, fileUris, 100L,
                50000L);
        mMms1 = getMmsDataObject(mContext, "mms-id1", mContact1, "MMS1 test subject",
                "MMS1 test message", RcsService.Direction.OUTGOING, timestamp++, fileUris, 101L,
                50000L);
        mMms2 = getMmsDataObject(mContext, "mms-id1", mContact2, "MMS2 test subject",
                "MMS2 test message", RcsService.Direction.OUTGOING, timestamp++, fileUris, 102L,
                50000L);
        mSms = new SmsDataObject("sms-id", mContact2, "SMS test message",
                RcsService.Direction.INCOMING, ReadStatus.UNREAD, timestamp++, 200L);
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

    public static MmsDataObject getMmsDataObject(Context ctx, String messageId, ContactId contact,
            String subject, String body, RcsService.Direction dir, long timestamp, List<Uri> files,
            Long nativeId, long maxFileIconSize) throws FileAccessException, MmsFileSizeException {
        List<MmsPart> mMmsParts = new ArrayList<>();
        for (Uri file : files) {
            String filename = FileUtils.getFileName(ctx, file);
            long fileSize = FileUtils.getFileSize(ctx, file);
            String extension = MimeManager.getFileExtension(filename);
            String mimeType = MimeManager.getInstance().getMimeType(extension);
            byte[] fileIcon = null;
            if (MimeManager.isImageType(mimeType)) {
                String imageFilename = FileUtils.getPath(ctx, file);
                fileIcon = ImageUtils.tryGetThumbnail(imageFilename, maxFileIconSize);
            }
            mMmsParts.add(new MmsPart(messageId, filename, fileSize, mimeType, file, fileIcon));
        }
        long availableSize = MMS_PARTS_MAX_FILE_SIZE;
        /* Remove size of the body text */
        if (body != null) {
            mMmsParts.add(new MmsPart(messageId, XmsMessageLog.MimeType.TEXT_MESSAGE, body));
            availableSize -= body.length();
        }
        /* remove size of the un-compressible parts */
        long imageSize = 0;
        for (MmsPart part : mMmsParts) {
            Long fileSize = part.getFileSize();
            if (fileSize != null) {
                /* The part is a file */
                if (!MimeManager.isImageType(part.getMimeType())) {
                    /* The part cannot be compressed: not an image */
                    availableSize -= fileSize;
                } else {
                    imageSize += fileSize;
                }
            }
        }
        if (availableSize < 0) {
            throw new MmsFileSizeException("Sum of un-compressible MMS parts is too high!");
        }
        if (imageSize > availableSize) {
            /* Image compression is required */
            Map<MmsPart, Long> imagesWithTargetSize = new HashMap<>();
            for (MmsPart part : mMmsParts) {
                if (MimeManager.isImageType(part.getMimeType())) {
                    Long targetSize = (part.getFileSize() * availableSize) / imageSize;
                    imagesWithTargetSize.put(part, targetSize);
                }
            }
            for (Map.Entry<MmsPart, Long> entry : imagesWithTargetSize.entrySet()) {
                MmsPart part = entry.getKey();
                Long maxSize = entry.getValue();
                String imagePath = FileUtils.getPath(ctx, part.getFile());
                part.setPdu(ImageUtils.compressImage(imagePath, maxSize, MAX_IMAGE_WIDTH,
                        MAX_IMAGE_HEIGHT));
                // images are compressed in jpeg format
                part.setMimeType("image/jpeg");
            }
        }
        return new MmsDataObject(messageId, contact, subject, dir, RcsService.ReadStatus.UNREAD,
                timestamp, nativeId, mMmsParts);
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
                RcsService.Direction.INCOMING, ReadStatus.READ, 1234L, 56789L);
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

    public void testUpdateMmsMessageId() throws FileAccessException {
        mXmsLog.addOutgoingMms(mMms1);
        ContactId contact = mMms1.getContact();
        String newMessageId = IdGenerator.generateMessageID();
        mXmsLog.updateMmsMessageId(contact, mMms1.getNativeId(), newMessageId);
        XmsDataObject xms = mXmsLog.getXmsDataObject(contact, newMessageId);
        assertTrue(xms instanceof MmsDataObject);
    }

    public void testSetMessageDelivered() throws FileAccessException {
        mXmsLog.addOutgoingMms(mMms1);
        ContactId contact = mMms1.getContact();
        String messageId = mMms1.getMessageId();
        long timestampDelivered = System.currentTimeMillis();
        mXmsLog.setMessageDelivered(contact, messageId, timestampDelivered);
        XmsDataObject xms = mXmsLog.getXmsDataObject(contact, messageId);
        assertTrue(xms instanceof MmsDataObject);
        assertEquals(timestampDelivered, xms.getTimestampDelivered());
    }

    public void testSetMessageSent() throws FileAccessException {
        mXmsLog.addOutgoingMms(mMms1);
        ContactId contact = mMms1.getContact();
        String messageId = mMms1.getMessageId();
        long timestampSent = System.currentTimeMillis();
        mXmsLog.setMessageSent(contact, messageId, timestampSent);
        XmsDataObject xms = mXmsLog.getXmsDataObject(contact, messageId);
        assertTrue(xms instanceof MmsDataObject);
        assertEquals(timestampSent, xms.getTimestampSent());
    }

    public void testSetStateAndReasonCode() throws FileAccessException {
        mXmsLog.addOutgoingMms(mMms1);
        ContactId contact = mMms1.getContact();
        String messageId = mMms1.getMessageId();
        mXmsLog.setStateAndReasonCode(contact, messageId, XmsMessage.State.FAILED,
                XmsMessage.ReasonCode.FAILED_MMS_ERROR_HTTP_FAILURE);
        XmsDataObject xms = mXmsLog.getXmsDataObject(contact, messageId);
        assertTrue(xms instanceof MmsDataObject);
        assertEquals(XmsMessage.State.FAILED, xms.getState());
        assertEquals(XmsMessage.ReasonCode.FAILED_MMS_ERROR_HTTP_FAILURE, xms.getReasonCode());
    }
}
