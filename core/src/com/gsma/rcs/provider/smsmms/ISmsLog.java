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
 *
 ******************************************************************************/

package com.gsma.rcs.provider.smsmms;

import com.gsma.rcs.provider.xms.model.SmsDataObject;

import android.database.Cursor;
import android.net.Uri;

import java.util.List;

/**
 * Interface for the SMS log accessor
 *
 * @author Philippe LEMORDANT
 */
public interface ISmsLog {

    /**
     * Gets the SMS from the native provider
     *
     * @param cursor the cursor
     * @param ntpLocalOffset the time offset to apply
     * @return the list of SMS objects
     */
    List<SmsDataObject> getSmsFromNativeProvider(Cursor cursor, String messageId,
            long ntpLocalOffset);

    /**
     * Gets the SMS from the native provider
     *
     * @param uri the SMS uri
     * @param messageId the message ID or null
     * @param ntpLocalOffset the time offset to apply
     * @return SmsDataObject
     */
    SmsDataObject getSmsFromNativeProvider(Uri uri, String messageId, long ntpLocalOffset);

    /**
     * Deletes SMS
     * 
     * @param nativeID the native ID
     * @return The number of rows deleted.
     */
    int deleteSms(long nativeID);
}
