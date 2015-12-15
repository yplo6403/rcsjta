/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.provider.xms;

import android.content.Context;
import android.content.OperationApplicationException;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.orange.labs.mms.priv.MmsFileSizeException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Philippe LEMORDANT on 16/11/2015.
 */
public class XmsLogTest extends InstrumentationTestCase {
    private AssetManager mAssetManager;
    private String mMessageId;
    private XmsLog mXmsLog;
    private ContactId mContact;
    private String mExternalDir;
    private Uri mUriCat1;
    private Uri mUriCat2;
    private Context mContext;
    private String mFileName1;
    private String mFileName2;
    private Long mFileSize1;
    private Long mFileSize2;
 
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        LocalContentResolver mLocalContentResolver = new LocalContentResolver(mContext.getContentResolver());
        mXmsLog = XmsLog.createInstance(mLocalContentResolver);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mContact = contactUtils.formatContact("+33786589041");
        mMessageId = "1234567890";
        mAssetManager = mContext.getAssets();
        mExternalDir = Environment.getExternalStorageDirectory().toString() + File.separator;
        mFileName1 = "cat-test1.jpg";
        createFileOnSdCard(mFileName1);
        File file = new File(mExternalDir + mFileName1);
        mFileSize1 = file.length();
        mUriCat1 = Uri.fromFile(file);
        mFileName2 = "cat-test2.jpg";
        file = new File(mExternalDir + mFileName2);
        mFileSize2 = file.length();
        createFileOnSdCard(mFileName2);
        mUriCat2 = Uri.fromFile(file);
    }

    private void createFileOnSdCard(String fileName) throws IOException {
        InputStream stream = mAssetManager.open(fileName);
        byte[] fileBytes = new byte[stream.available()];
        stream.read(fileBytes);
        stream.close();
        fileName = mExternalDir + fileName;
        File catFile = new File(fileName);
        if (catFile.exists()) {
            catFile.delete();
        }
        FileOutputStream fos = new FileOutputStream(catFile, true);
        fos.write(fileBytes);
        fos.close();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mXmsLog.deleteXmsMessage(mMessageId);
    }

    public void testSmsMessage() {
        long timestamp = System.currentTimeMillis();
        SmsDataObject sms = new SmsDataObject(mMessageId, mContact, "SMS test message",
                RcsService.Direction.OUTGOING, ReadStatus.UNREAD, timestamp, null, null);
        mXmsLog.addSms(sms);
        Cursor cursor = mXmsLog.getXmsMessage(mMessageId);
        assertEquals(cursor.getCount(), 1);
        assertTrue(cursor.moveToNext());

        String id = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
        assertEquals(mMessageId, id);

        String contact = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
        assertEquals(mContact.toString(), contact);

        RcsService.Direction direction = RcsService.Direction.valueOf(cursor.getInt(cursor
                .getColumnIndex(XmsMessageLog.DIRECTION)));
        assertEquals(sms.getDirection(), direction);

        String body = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTENT));
        assertNotNull(body);

        String mimeType = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MIME_TYPE));
        assertEquals(XmsMessageLog.MimeType.TEXT_MESSAGE, mimeType);

        long readTimestamp = cursor.getLong(cursor.getColumnIndex(XmsMessageLog.TIMESTAMP));
        assertEquals(timestamp, readTimestamp);

        long state = cursor.getLong(cursor.getColumnIndex(XmsMessageLog.STATE));
        assertEquals(XmsMessage.State.QUEUED.toInt(), state);

        long reason = cursor.getLong(cursor.getColumnIndex(XmsMessageLog.REASON_CODE));
        assertEquals(XmsMessage.ReasonCode.UNSPECIFIED.toInt(), reason);

        long readStatus = cursor.getLong(cursor.getColumnIndex(XmsMessageLog.READ_STATUS));
        assertEquals(RcsService.ReadStatus.UNREAD.toInt(), readStatus);
        cursor.close();
    }

    public void testMmsMessage() throws IOException, RemoteException, OperationApplicationException, MmsFileSizeException, FileAccessException {
        long timestamp = System.currentTimeMillis();
        List<Uri> files = new ArrayList<>();
        files.add(mUriCat1);
        files.add(mUriCat2);
        MmsDataObject mms = new MmsDataObject(mContext, "mms_id", mMessageId, mContact,
                "MMS test subject", "MMS test message", RcsService.Direction.INCOMING,
                timestamp, files, null, 50000L);
        mXmsLog.addMms(mms);
        Cursor cursor = mXmsLog.getXmsMessage(mMessageId);
        assertEquals(cursor.getCount(), 1);
        assertTrue(cursor.moveToNext());

        String id = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
        assertEquals(mMessageId, id);

        String contact = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
        assertEquals(mContact.toString(), contact);

        RcsService.Direction direction = RcsService.Direction.valueOf(cursor.getInt(cursor
                .getColumnIndex(XmsMessageLog.DIRECTION)));
        assertEquals(mms.getDirection(), direction);

        String body = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTENT));
        assertNotNull(body);

        String mimeType = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MIME_TYPE));
        assertEquals(XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, mimeType);

        long readTimestamp = cursor.getLong(cursor.getColumnIndex(XmsMessageLog.TIMESTAMP));
        assertEquals(timestamp, readTimestamp);

        long state = cursor.getLong(cursor.getColumnIndex(XmsMessageLog.STATE));
        assertEquals(XmsMessage.State.QUEUED.toInt(), state);

        long reason = cursor.getLong(cursor.getColumnIndex(XmsMessageLog.REASON_CODE));
        assertEquals(XmsMessage.ReasonCode.UNSPECIFIED.toInt(), reason);

        long readStatus = cursor.getLong(cursor.getColumnIndex(XmsMessageLog.READ_STATUS));
        assertEquals(RcsService.ReadStatus.UNREAD.toInt(), readStatus);
        cursor.close();
        /* test parts */
        Set<MmsPart> parts = mXmsLog.getParts(mMessageId);
        assertEquals(parts.size(), 3);
        for (MmsPart part : parts) {
            assertEquals(mContact, part.getContact());

            assertEquals(mMessageId, part.getMessageId());
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

    public void testDeleteMmsMessageId() throws RemoteException, OperationApplicationException,
            IOException, MmsFileSizeException, FileAccessException {
        long timestamp = System.currentTimeMillis();
        MmsDataObject mms = new MmsDataObject(mContext, "mms_id", mMessageId, mContact,
                "MMS test subject", "MMS test message", RcsService.Direction.INCOMING,
                timestamp, new ArrayList<Uri>(), null,50000L);
        mXmsLog.addMms(mms);
        Cursor cursor = mXmsLog.getXmsMessage(mMessageId);
        assertEquals(1, cursor.getCount());
        cursor.close();
        Set<MmsPart> parts = mXmsLog.getParts(mMessageId);
        assertTrue(parts.size() > 0);
        mXmsLog.deleteXmsMessage(mMessageId);
        cursor = mXmsLog.getXmsMessage(mMessageId);
        assertEquals(0, cursor.getCount());
        cursor.close();
        parts = mXmsLog.getParts(mMessageId);
        assertEquals(0, parts.size());
    }

    public void testDeleteSmsMessageId() {
        long timestamp = System.currentTimeMillis();
        SmsDataObject sms = new SmsDataObject(mMessageId, mContact, "SMS test message",
                RcsService.Direction.INCOMING, ReadStatus.UNREAD, timestamp, null, null);
        mXmsLog.addSms(sms);
        Cursor cursor = mXmsLog.getXmsMessage(mMessageId);
        assertEquals(cursor.getCount(), 1);
        cursor.close();
        mXmsLog.deleteXmsMessage(mMessageId);
        cursor = mXmsLog.getXmsMessage(mMessageId);
        assertEquals(cursor.getCount(), 0);
        cursor.close();
    }
}
