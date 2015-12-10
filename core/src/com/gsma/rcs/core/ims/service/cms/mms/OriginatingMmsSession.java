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

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.MmsDataObject.MmsPart;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;
import com.orange.labs.mms.MmsMessage;
import com.orange.labs.mms.priv.MmsApnConfigException;
import com.orange.labs.mms.priv.MmsConnectivityException;
import com.orange.labs.mms.priv.MmsException;
import com.orange.labs.mms.priv.MmsFileSizeException;
import com.orange.labs.mms.priv.MmsFormatException;
import com.orange.labs.mms.priv.MmsHttpException;
import com.orange.labs.mms.priv.MmsIOException;
import com.orange.labs.mms.priv.utils.MmsApn;
import com.orange.labs.mms.priv.utils.MmsEncoderUtils;
import com.orange.labs.mms.priv.utils.NetworkUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by yplo6403 on 07/12/2015.
 */
public class OriginatingMmsSession implements Runnable, MmsSessionListener {

    private static final Logger sLogger = Logger.getLogger(OriginatingMmsSession.class
            .getSimpleName());

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final String mMmsId;
    private final ContactId mContact;
    private final String mSubject;
    private final XmsLog mXmsLog;
    private final Set<MmsDataObject.MmsPart> mParts;
    private final List<MmsSessionListener> mListeners;

    /**
     * @param context context
     * @param contentResolver contentResolver
     * @param xmsLog The XMS log accessor
     * @param mmsId The message ID
     * @param contact The remote contact
     * @param subject The subject
     * @param parts The MMS attachment parts
     */
    public OriginatingMmsSession(Context context, ContentResolver contentResolver, XmsLog xmsLog, String mmsId, ContactId contact, String subject,
            Set<MmsDataObject.MmsPart> parts) {
        mContext = context;
        mContentResolver = contentResolver;
        mXmsLog = xmsLog;
        mMmsId = mmsId;
        mContact = contact;
        mSubject = subject;
        mParts = parts;
        mListeners = new ArrayList<>();
    }

    @Override
    public void run() {
        boolean success = false;
        try {
            boolean logActivated = sLogger.isActivated();
            if (logActivated) {
                sLogger.debug("Send MMS ID " + mMmsId + " to contact " + mContact + " subject='"
                        + mSubject + "'");
            }
            onMmsTransferStarted(mContact, mMmsId);
            MmsMessage msg = new MmsMessage();
            msg.addTo(mContact.toString());
            msg.setSubject(mSubject);
            for(MmsPart part : mParts){
                String mimeType = part.getMimeType();
                if(MimeManager.isImageType(mimeType)){
                    msg.attachBitmap(BitmapFactory.decodeStream(mContentResolver.openInputStream(part.getFile())));
                }
                else{
                    msg.attach(mimeType, part.getBody().getBytes());
                }
            }
            byte[] encMsg = MmsEncoderUtils.encodeMessage(msg);
            NetworkUtils.startMmsConnectivity(mContext);
            // Get APNs
            List<MmsApn> apns = MmsApn.getMmsAPNs(mContext);
            // For each APN, try to send the message
            for (MmsApn apn :apns)
            {
                if(logActivated){
                    sLogger.debug("Trying apn : " + apn.toString());
                }
                // Check the route
                String routeHost = apn.isProxySet ? apn.proxyHost : Uri.parse(apn.mmsc).getHost();
                if (NetworkUtils.ensureRoute(mContext, routeHost)) {
                    success = NetworkUtils.sendMessage(apn, encMsg);
                    if(success){
                        break;
                    }
                }
            }
            if(success){
                onMmsTransferred(mContact, mMmsId);
            }
            else{
                onMmsTransferError(ReasonCode.FAILED_MMS_ERROR_UNABLE_CONNECT_MMS, mContact, mMmsId);
            }

        } catch (MmsException | FileNotFoundException e) {
            ReasonCode reasonCode = ReasonCode.FAILED_MMS_ERROR_UNSPECIFIED;
            if(e instanceof MmsApnConfigException){
                reasonCode = ReasonCode.FAILED_MMS_ERROR_INVALID_APN;
            }
            else if (e instanceof MmsConnectivityException){
                reasonCode = ReasonCode.FAILED_MMS_ERROR_UNABLE_CONNECT_MMS;
            }
            else if(e instanceof MmsFileSizeException || e instanceof MmsFormatException){
                reasonCode = ReasonCode.FAILED_ERROR_GENERIC_FAILURE;
            }
            else if( e instanceof MmsHttpException || e instanceof MmsIOException){
                reasonCode = ReasonCode.FAILED_MMS_ERROR_HTTP_FAILURE;
            }
            else if( e instanceof MmsIOException){
                reasonCode = ReasonCode.FAILED_MMS_ERROR_IO_ERROR;
            }
            onMmsTransferError(reasonCode, mContact, mMmsId);
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to send MMS!", e);
        }
        finally {
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
