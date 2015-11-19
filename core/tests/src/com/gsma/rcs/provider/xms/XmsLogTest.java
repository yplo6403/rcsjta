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

import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import java.io.IOException;

/**
 * Created by Philippe LEMORDANT on 16/11/2015.
 */
public class XmsLogTest extends AndroidTestCase {
    private String mMessageId;
    private XmsLog mXmsLog;
    private ContactId mContact;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        ContentResolver mContentResolver = mContext.getContentResolver();
        LocalContentResolver mLocalContentResolver = new LocalContentResolver(mContentResolver);
        mXmsLog = XmsLog.createInstance(mContentResolver, mLocalContentResolver);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
        mContact = contactUtils.formatContact("+339000000");
        mMessageId = "1234567890";
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mXmsLog.deleteAllEntries();
    }

    public void testSmsMessage() {
        long timestamp = System.currentTimeMillis();
        SmsDataObject sms = new SmsDataObject(mMessageId, mContact, "SMS test message", RcsService.Direction.INCOMING, timestamp, 1234567890);
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
        assertEquals(RcsService.Direction.INCOMING, direction);

        String body = cursor.getString(cursor.getColumnIndex(XmsMessageLog.BODY));
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
    }

    public void testMmsMessage() throws IOException, RemoteException, OperationApplicationException {
        long timestamp = System.currentTimeMillis();
        // TODO use robot electric to test list of image URIs
        MmsDataObject sms = new MmsDataObject(mContext, "mms_id", mMessageId, mContact, "MMS test message",
                RcsService.Direction.INCOMING, timestamp, null, 1234567890);
        mXmsLog.addMms(sms);
        Cursor cursor = mXmsLog.getXmsMessage(mMessageId);
        assertEquals(cursor.getCount(), 1);
        assertTrue(cursor.moveToNext());

        String id = cursor.getString(cursor.getColumnIndex(XmsMessageLog.MESSAGE_ID));
        assertEquals(mMessageId, id);

        String contact = cursor.getString(cursor.getColumnIndex(XmsMessageLog.CONTACT));
        assertEquals(mContact.toString(), contact);

        RcsService.Direction direction = RcsService.Direction.valueOf(cursor.getInt(cursor
                .getColumnIndex(XmsMessageLog.DIRECTION)));
        assertEquals(RcsService.Direction.INCOMING, direction);

        String body = cursor.getString(cursor.getColumnIndex(XmsMessageLog.BODY));
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

        //TODO test parts content
    }
}
