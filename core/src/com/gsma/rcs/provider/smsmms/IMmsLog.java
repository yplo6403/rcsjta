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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.xms.mms.MmsFileSizeException;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.services.rcs.contact.ContactId;

import com.klinker.android.send_message.Message;

import android.net.Uri;

import java.util.List;

/**
 * Interface for the MMS log accessor
 *
 * @author Philippe LEMORDANT
 */
public interface IMmsLog {

    /**
     * Gets the MMS from the native provider
     *
     * @param id the native ID
     * @param ntpLocalOffset the time offset to apply
     * @return the list of MMS object because if sent to multiple contacts, MMS pdu is duplicated.
     */
    List<MmsDataObject> getMmsFromNativeProvider(long id, long ntpLocalOffset);

    /**
     * Gets the MMS message to be sent and performs image compression if required
     *
     * @param contact the remtoe contact
     * @param files the files to be sent
     * @param subject the subject
     * @param body the body text
     * @return Message
     * @throws MmsFileSizeException
     * @throws FileAccessException
     */
    Message getMms(ContactId contact, List<Uri> files, final String subject, final String body)
            throws MmsFileSizeException, FileAccessException;

    /**
     * Deletes MMS
     *
     * @param nativeID the native ID
     * @return The number of rows deleted.
     */
    int deleteMms(long nativeID);
}
