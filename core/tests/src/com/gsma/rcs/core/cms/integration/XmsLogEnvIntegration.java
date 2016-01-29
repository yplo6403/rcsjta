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

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.xms.XmsData;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.cms.XmsMessageLog.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class XmsLogEnvIntegration {

    private static final String SORT_BY_DATE_DESC = new StringBuilder(XmsData.KEY_TIMESTAMP)
            .append(" DESC").toString();

    private static final String SELECTION_XMS_CONTACT = XmsData.KEY_CONTACT + "=?" + " AND "
            + XmsData.KEY_MIME_TYPE + "=?";

    protected final LocalContentResolver mLocalContentResolver;

    /**
     * Current instance
     */
    private static volatile XmsLogEnvIntegration sInstance;

    private XmsLogEnvIntegration(Context context) {
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());
    }

    public static XmsLogEnvIntegration getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (XmsLogEnvIntegration.class) {
            if (sInstance == null) {
                sInstance = new XmsLogEnvIntegration(context);
            }
        }
        return sInstance;
    }

    /**
     * @param contact
     * @return SmsData
     */
    public List getMessages(String mimeType, ContactId contact) {

        List<XmsDataObject> messages = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(XmsData.CONTENT_URI, null, SELECTION_XMS_CONTACT,
                    new String[] {
                            contact.toString(), mimeType
                    }, SORT_BY_DATE_DESC);
            CursorUtil.assertCursorIsNotNull(cursor, XmsData.CONTENT_URI);

            int messageIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_ID);
            int nativeProviderIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_ID);
            int nativeThreadIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_NATIVE_THREAD_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
            int contentIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTENT);
            int dateIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_TIMESTAMP);
            int directionIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_DIRECTION);
            int readStatusIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_READ_STATUS);
            int mmsIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MMS_ID);
            List<MmsPart> parts = new ArrayList<>();
            while (cursor.moveToNext()) {
                if (MimeType.TEXT_MESSAGE.equals(mimeType)) {
                    messages.add(new SmsDataObject(cursor.getString(messageIdIdx), ContactUtil
                            .createContactIdFromTrustedData(cursor.getString(contactIdx)), cursor
                            .getString(contentIdx), RcsService.Direction.valueOf(cursor
                            .getInt(directionIdx)),
                            ReadStatus.valueOf(cursor.getInt(readStatusIdx)), cursor
                                    .getLong(dateIdx), cursor.isNull(nativeProviderIdIdx) ? null
                                    : cursor.getLong(nativeProviderIdIdx), cursor
                                    .isNull(nativeThreadIdIdx) ? null : cursor
                                    .getLong(nativeThreadIdIdx)));
                } else {
                    messages.add(new MmsDataObject(cursor.getString(mmsIdIdx), cursor
                            .getString(messageIdIdx), ContactUtil
                            .createContactIdFromTrustedData(cursor.getString(contactIdx)), cursor
                            .getString(contentIdx), RcsService.Direction.valueOf(cursor
                            .getInt(directionIdx)),
                            ReadStatus.valueOf(cursor.getInt(readStatusIdx)), cursor
                                    .getLong(dateIdx), cursor.isNull(nativeProviderIdIdx) ? null
                                    : cursor.getLong(nativeProviderIdIdx), cursor
                                    .isNull(nativeThreadIdIdx) ? null : cursor
                                    .getLong(nativeThreadIdIdx), parts));
                }
            }
            return messages;
        } finally {
            CursorUtil.close(cursor);
        }
    }
}
