/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.smsmms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.xms.mms.MmsFileSizeException;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.services.rcs.contact.ContactId;

import com.klinker.android.send_message.Message;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.List;

/**
 * A class to manage access to native SMS/MMS providers.
 *
 * @author Philippe LEMORDANT
 */
public class SmsMmsLog implements ISmsLog, IMmsLog {

    /**
     * Current instance
     */
    private static volatile SmsMmsLog sInstance;
    private final SmsLog mSmsLog;
    private final MmsLog mMmsLog;

    private SmsMmsLog(Context ctx, ContentResolver resolver) {
        mSmsLog = new SmsLog(resolver);
        mMmsLog = new MmsLog(ctx, resolver);
    }

    /**
     * Get or Create Singleton instance of SmsMmsLog
     *
     * @param ctx the context
     * @param resolver the content resolver
     * @return singleton instance
     */
    public static SmsMmsLog getInstance(Context ctx, ContentResolver resolver) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (SmsMmsLog.class) {
            if (sInstance == null) {
                sInstance = new SmsMmsLog(ctx, resolver);
            }
            return sInstance;
        }
    }

    @Override
    public List<SmsDataObject> getSmsFromNativeProvider(Cursor cursor, String messageId,
            long ntpLocalOffset) {
        return mSmsLog.getSmsFromNativeProvider(cursor, messageId, ntpLocalOffset);
    }

    @Override
    public SmsDataObject getSmsFromNativeProvider(Uri uri, String messageId, long ntpLocalOffset) {
        return mSmsLog.getSmsFromNativeProvider(uri, messageId, ntpLocalOffset);
    }

    @Override
    public int deleteSms(long nativeID) {
        return mSmsLog.deleteSms(nativeID);
    }

    @Override
    public boolean markSmsAsRead(Long nativeId) {
        return mSmsLog.markSmsAsRead(nativeId);
    }

    @Override
    public List<MmsDataObject> getMmsFromNativeProvider(long id, long ntpLocalOffset) {
        return mMmsLog.getMmsFromNativeProvider(id, ntpLocalOffset);
    }

    @Override
    public Message getMms(ContactId contact, List<Uri> files, String subject, String body)
            throws MmsFileSizeException, FileAccessException {
        return mMmsLog.getMms(contact, files, subject, body);
    }

    @Override
    public int deleteMms(long nativeID) {
        return mMmsLog.deleteMms(nativeID);
    }

    @Override
    public boolean markMmsAsRead(Long nativeId) {
        return mMmsLog.markMmsAsRead(nativeId);
    }

}
