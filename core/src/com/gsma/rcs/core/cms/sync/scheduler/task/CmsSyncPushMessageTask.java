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

package com.gsma.rcs.core.cms.sync.scheduler.task;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.protocol.cmd.ImapFolder;
import com.gsma.rcs.core.cms.protocol.message.IImapMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapMmsMessage;
import com.gsma.rcs.core.cms.protocol.message.ImapSmsMessage;
import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncSchedulerTask;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.PushStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task executed to push messages on the CMS server
 */
public class CmsSyncPushMessageTask extends CmsSyncSchedulerTask {

    private static final Logger sLogger = Logger.getLogger(CmsSyncPushMessageTask.class
            .getSimpleName());

    /* package private */final PushMessageTaskListener mListener;
    /* package private */final RcsSettings mRcsSettings;
    /* package private */final Context mContext;
    /* package private */final XmsLog mXmsLog;
    /* package private */final CmsLog mCmsLog;
    /* package private */final ContactId mContact;

    /* package private */final Map<String, Integer> mCreatedUidsMap;

    /**
     * Call this constructor when executing this task in a synchronous way
     *
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param xmsLog the XMS log accessor
     * @param cmsLog the CMS log accessor
     */
    public CmsSyncPushMessageTask(Context context, RcsSettings rcsSettings, XmsLog xmsLog,
            CmsLog cmsLog) {
        this(context, rcsSettings, xmsLog, cmsLog, null, null);
    }

    /**
     * Call this constructor when executing this task as a runnable (run method executed)
     *
     * @param context the context
     * @param rcsSettings the RCS settings accessor
     * @param xmsLog the XMS log accessor
     * @param cmsLog the CMS log accessor
     * @param contact the contact
     * @param listener callback listener
     */
    public CmsSyncPushMessageTask(Context context, RcsSettings rcsSettings, XmsLog xmsLog,
            CmsLog cmsLog, ContactId contact, PushMessageTaskListener listener) {
        mRcsSettings = rcsSettings;
        mContext = context;
        mXmsLog = xmsLog;
        mCmsLog = cmsLog;
        mContact = contact;
        mListener = listener;
        mCreatedUidsMap = new HashMap<>();
    }

    @Override
    public void run() {
        try {
            List<XmsDataObject> messagesToPush = new ArrayList<>();
            String folder = CmsUtils.contactToCmsFolder(mRcsSettings, mContact);
            for (CmsObject cmsObject : mCmsLog.getXmsMessages(folder, PushStatus.PUSH_REQUESTED)) {
                XmsDataObject xms = mXmsLog.getXmsDataObject(cmsObject.getMessageId());
                if (xms != null) {
                    messagesToPush.add(xms);
                }
            }
            if (messagesToPush.isEmpty()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("no message to push");
                }
                return;
            }
            pushMessages(messagesToPush);

        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.info(e.getMessage());
            }

        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Failed to push message", e);

        } finally {
            if (mListener != null) {
                mListener.onPushMessageTaskCallbackExecuted(mCreatedUidsMap);
            }
        }
    }

    /**
     * Push messages.
     *
     * @param messages the list of messages to push
     */
    public void pushMessages(List<XmsDataObject> messages) throws NetworkException,
            PayloadException {
        String from, to, direction;
        from = to = direction = null;
        try {
            /*
             * Sort list of XMS objects by timestamp so that UIDs generated by CMS will be aligned
             * with the chronological order. This allows correlation for SMS messages.
             */
            Collections.sort(messages, new Comparator<XmsDataObject>() {
                @Override
                public int compare(XmsDataObject obj1, XmsDataObject obj2) {
                    return Long.valueOf(obj1.getTimestamp()).compareTo(obj2.getTimestamp());
                }
            });
            List<String> existingFolders = new ArrayList<>();
            for (ImapFolder imapFolder : getBasicImapService().listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            String prevSelectedFolder = "";
            for (XmsDataObject message : messages) {
                List<Flag> flags = new ArrayList<>();
                switch (message.getDirection()) {
                    case INCOMING:
                        from = CmsUtils.contactToHeader(message.getContact());
                        to = CmsUtils.contactToHeader(mRcsSettings.getUserProfileImsUserName());
                        direction = Constants.DIRECTION_RECEIVED;
                        break;
                    case OUTGOING:
                        from = CmsUtils.contactToHeader(mRcsSettings.getUserProfileImsUserName());
                        to = CmsUtils.contactToHeader(message.getContact());
                        direction = Constants.DIRECTION_SENT;
                        break;
                    default:
                        break;
                }
                if (message.getReadStatus() != ReadStatus.UNREAD) {
                    flags.add(Flag.Seen);
                }

                IImapMessage imapMessage = null;

                if (message instanceof SmsDataObject) {
                    SmsDataObject sms = (SmsDataObject) message;
                    imapMessage = new ImapSmsMessage(from, to, direction, sms.getTimestamp(),
                            sms.getBody(), UUID.randomUUID().toString(), UUID.randomUUID()
                                    .toString(), "" + message.getTimestamp());
                } else if (message instanceof MmsDataObject) {
                    MmsDataObject mms = (MmsDataObject) message;
                    imapMessage = new ImapMmsMessage(mContext, from, to, direction,
                            mms.getTimestamp(), mms.getSubject(), UUID.randomUUID().toString(),
                            UUID.randomUUID().toString(), "" + mms.getTimestamp(), mms.getMmsId(),
                            mms.getMmsParts());
                }
                String remoteFolder = CmsUtils.contactToCmsFolder(mRcsSettings,
                        message.getContact());
                if (!existingFolders.contains(remoteFolder)) {
                    getBasicImapService().create(remoteFolder);
                    existingFolders.add(remoteFolder);
                }
                if (!remoteFolder.equals(prevSelectedFolder)) {
                    getBasicImapService().selectCondstore(remoteFolder);
                    prevSelectedFolder = remoteFolder;
                }
                int uid = getBasicImapService()
                        .append(remoteFolder, flags, imapMessage.toPayload());
                mCreatedUidsMap.put(message.getMessageId(), uid);
            }
        } catch (IOException e) {
            throw new NetworkException("Failed to push messages", e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to push messages", e);
        }
    }

    public Map<String, Integer> getCreatedUids() {
        return mCreatedUidsMap;
    }

    /**
     * Interface used to notify listeners when messages have been pushed on the CMS server (when
     * call in an asynchronous way)
     */
    public interface PushMessageTaskListener {
        /**
         * Callback method
         *
         * @param uids created for the pushed messages
         */
        void onPushMessageTaskCallbackExecuted(Map<String, Integer> uids);
    }
}
