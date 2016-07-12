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

package com.gsma.rcs.core.cms.xms.mms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.xms.XmsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yplo6403 on 07/12/2015.
 */
public final class OriginatingMmsSession implements Runnable, MmsSessionListener {

    private static final Logger sLogger = Logger.getLogger(OriginatingMmsSession.class
            .getSimpleName());

    private final Context mCtx;
    private final String mMmsId;
    private final ContactId mContact;
    private final String mSubject;
    private final List<MmsDataObject.MmsPart> mParts;
    private final List<MmsSessionListener> mListeners;
    private final RcsSettings mRcsSettings;
    private final XmsManager mXmsManager;

    /**
     * @param ctx context
     * @param mmsId The message ID
     * @param contact The remote contact
     * @param subject The subject
     * @param parts The MMS attachment parts
     * @param rcsSettings The RCS settings accessor
     */
    public OriginatingMmsSession(Context ctx, String mmsId, ContactId contact, String subject,
            List<MmsDataObject.MmsPart> parts, RcsSettings rcsSettings, XmsManager xmsManager) {
        mCtx = ctx;
        mMmsId = mmsId;
        mContact = contact;
        mSubject = subject;
        mParts = parts;
        mListeners = new ArrayList<>();
        mRcsSettings = rcsSettings;
        mXmsManager = xmsManager;
    }

    @Override
    public void run() {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Send MMS ID " + mMmsId + " to contact " + mContact + " subject='"
                    + mSubject + "'");
        }
        try {
            onMmsTransferStarted(mContact, mMmsId);
            ContactId sender = mRcsSettings.getUserProfileImsUserName();
            /*
             * We use the messageId as the transaction ID to be able to link local XMS provider and
             * MMS native provider.
             */
            MmsEncodedMessage mmsEncodedMessage = new MmsEncodedMessage(mCtx, sender, mContact,
                    mSubject, mMmsId, mParts);
            mXmsManager.sendMms(mMmsId, mContact, mmsEncodedMessage.encode(), this);

        } catch (MmsFormatException e) {
            sLogger.error("Failed to format MMS!", e);
            onMmsTransferError(ReasonCode.FAILED_ERROR_GENERIC_FAILURE, mContact, mMmsId);

        } catch (FileAccessException e) {
            sLogger.error("Cannot find MMS part!", e);
            onMmsTransferError(ReasonCode.FAILED_MMS_ERROR_PART_NOT_FOUND, mContact, mMmsId);

        } catch (IOException e) {
            sLogger.error("Cannot write MMS pdu!", e);
            onMmsTransferError(ReasonCode.FAILED_MMS_ERROR_IO_ERROR, mContact, mMmsId);

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to send MMS!", e);
            onMmsTransferError(ReasonCode.UNSPECIFIED, mContact, mMmsId);
        }
    }

    public void addListener(MmsSessionListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void onMmsTransferError(ReasonCode reason, ContactId contact, String mmsId) {
        for (MmsSessionListener listener : mListeners) {
            listener.onMmsTransferError(reason, contact, mmsId);
        }
    }

    @Override
    public void onMmsTransferred(ContactId contact, String mmsId) {
        for (MmsSessionListener listener : mListeners) {
            listener.onMmsTransferred(contact, mmsId);
        }
    }

    @Override
    public void onMmsTransferStarted(ContactId contact, String mmsId) {
        for (MmsSessionListener listener : mListeners) {
            listener.onMmsTransferStarted(contact, mmsId);
        }
    }
}
