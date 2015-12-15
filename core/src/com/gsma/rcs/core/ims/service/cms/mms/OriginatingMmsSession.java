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

package com.gsma.rcs.core.ims.service.cms.mms;

import android.content.Context;
import android.net.Uri;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;
import com.orange.labs.mms.MmsMessage;
import com.orange.labs.mms.priv.MmsApnConfigException;
import com.orange.labs.mms.priv.MmsConnectivityException;
import com.orange.labs.mms.priv.MmsFormatException;
import com.orange.labs.mms.priv.MmsHttpException;
import com.orange.labs.mms.priv.PartMMS;
import com.orange.labs.mms.priv.parser.MmsEncodedMessage;
import com.orange.labs.mms.priv.utils.MmsApn;
import com.orange.labs.mms.priv.utils.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by yplo6403 on 07/12/2015.
 */
public final class OriginatingMmsSession implements Runnable, MmsSessionListener {

    private static final Logger sLogger = Logger.getLogger(OriginatingMmsSession.class
            .getSimpleName());

    private final Context mContext;
    private final String mMmsId;
    private final ContactId mContact;
    private final String mSubject;
    private final Set<MmsDataObject.MmsPart> mParts;
    private final List<MmsSessionListener> mListeners;
    private final RcsSettings mRcsSettings;

    /**
     * @param context context
     * @param mmsId The message ID
     * @param contact The remote contact
     * @param subject The subject
     * @param parts The MMS attachment parts
     * @param rcsSettings The RCS settings accessor
     */
    public OriginatingMmsSession(Context context, String mmsId, ContactId contact, String subject,
            Set<MmsDataObject.MmsPart> parts, RcsSettings rcsSettings) {
        mContext = context;
        mMmsId = mmsId;
        mContact = contact;
        mSubject = subject;
        mParts = parts;
        mListeners = new ArrayList<>();
        mRcsSettings = rcsSettings;
    }

    @Override
    public void run() {
        boolean success = false;
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Send MMS ID " + mMmsId + " to contact " + mContact + " subject='"
                    + mSubject + "'");
        }
        try {
            onMmsTransferStarted(mContact, mMmsId);
            List<PartMMS> partsMms = new ArrayList<>();
            for (MmsPart part : mParts) {
                String mimeType = part.getMimeType();
                byte[] content;
                String body = part.getContentText();
                if (body != null) {
                    content = body.getBytes();
                } else {
                    content = part.getCompressed();
                    if (content == null) {
                        String filePath = FileUtils.getPath(mContext, part.getFile());
                        try {
                            content = FileUtils.getContent(filePath);
                        } catch (IOException e) {
                            throw new FileAccessException("Failed to read part: " + filePath, e);
                        }
                    }
                }
                partsMms.add(new PartMMS(mimeType, content));
            }
            MmsMessage msg = new MmsMessage(mRcsSettings.getUserProfileImsUserName(),
                                        Collections.singletonList(mContact), mSubject, partsMms);

            MmsEncodedMessage mmsEncodedMessage = new MmsEncodedMessage(msg);
            NetworkUtils.startMmsConnectivity(mContext);
            // Get APNs
            List<MmsApn> apns = MmsApn.getMmsAPNs(mContext);
            // For each APN, try to send the message
            for (MmsApn apn : apns) {
                if (logActivated) {
                    sLogger.debug("Trying APN : " + apn.toString());
                }
                // Check the route
                String routeHost = apn.isProxySet ? apn.proxyHost : Uri.parse(apn.mmsc).getHost();
                if (NetworkUtils.ensureRoute(mContext, routeHost)) {
                    success = NetworkUtils.sendMessage(apn, mmsEncodedMessage.encode());
                    if (success) {
                        break;
                    }
                }
            }
            if (success) {
                onMmsTransferred(mContact, mMmsId);
            } else {
                onMmsTransferError(ReasonCode.FAILED_MMS_ERROR_UNABLE_CONNECT_MMS, mContact, mMmsId);
            }

        } catch (MmsApnConfigException e) {
            onMmsTransferError(ReasonCode.FAILED_MMS_ERROR_INVALID_APN, mContact, mMmsId);

        } catch (MmsConnectivityException e) {
            onMmsTransferError(ReasonCode.FAILED_MMS_ERROR_UNABLE_CONNECT_MMS, mContact, mMmsId);

        } catch (MmsFormatException e) {
            sLogger.error("Failed to format MMS!", e);
            onMmsTransferError(ReasonCode.FAILED_ERROR_GENERIC_FAILURE, mContact, mMmsId);

        } catch (MmsHttpException e) {
            sLogger.error("Failed to send MMS over HTTP!", e);
            onMmsTransferError(ReasonCode.FAILED_MMS_ERROR_HTTP_FAILURE, mContact, mMmsId);

        } catch (FileAccessException e) {
            sLogger.error("Cannot find MMS part!", e);
            onMmsTransferError(ReasonCode.FAILED_MMS_ERROR_PART_NOT_FOUND, mContact, mMmsId);

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to send MMS!", e);
            onMmsTransferError(ReasonCode.UNSPECIFIED, mContact, mMmsId);

        } finally {
            NetworkUtils.endConnectivity(mContext);
        }
    }

    public void addListener(MmsSessionListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void onMmsTransferError(ReasonCode reason, ContactId contact, String mmsId) {
        for(MmsSessionListener listener : mListeners){
            listener.onMmsTransferError(reason, contact, mmsId);
        }
    }

    @Override
    public void onMmsTransferred(ContactId contact, String mmsId) {
        for(MmsSessionListener listener : mListeners){
            listener.onMmsTransferred(contact, mmsId);
        }
    }

    @Override
    public void onMmsTransferStarted(ContactId contact, String mmsId) {
        for(MmsSessionListener listener : mListeners){
            listener.onMmsTransferStarted(contact, mmsId);
        }
    }
}
