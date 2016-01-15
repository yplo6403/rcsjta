/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.IImapMessage;
import com.gsma.rcs.cms.imap.message.ImapMmsMessage;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.provider.xms.model.XmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.ReadStatus;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task executed to push messages on the CMS server
 */
public class PushMessageTask implements Runnable {

    private static final Logger sLogger = Logger.getLogger(PushMessageTask.class.getSimpleName());

    /* package private */final PushMessageTaskListener mListener;
    /* package private */final ImapServiceController mImapServiceController;
    /* package private */final RcsSettings mRcsSettings;
    /* package private */final Context mContext;
    /* package private */final XmsLog mXmsLog;
    /* package private */final ImapLog mImapLog;

    /* package private */final Map<String, Integer> mCreatedUidsMap;

    /**
     * @param imapServiceController
     * @param xmsLog
     * @param listener
     */
    public PushMessageTask(Context context, RcsSettings rcsSettings,
            ImapServiceController imapServiceController, XmsLog xmsLog, ImapLog imapLog,
            PushMessageTaskListener listener) {
        mRcsSettings = rcsSettings;
        mContext = context;
        mImapServiceController = imapServiceController;
        mXmsLog = xmsLog;
        mImapLog = imapLog;
        mListener = listener;
        mCreatedUidsMap = new HashMap<>();
    }

    @Override
    public void run() {
        try {
            List<XmsDataObject> messagesToPush = new ArrayList<>();
            for (MessageData messageData : mImapLog.getXmsMessages(PushStatus.PUSH_REQUESTED)) {
                XmsDataObject xms = mXmsLog.getXmsDataObject(messageData.getMessageId());
                if (xms != null) {
                    messagesToPush.add(xms);
                }
            }
            if (messagesToPush.isEmpty()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("no message to push");
                }
            }
            mImapServiceController.createService();
            pushMessages(messagesToPush);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mImapServiceController.closeService();
            if (mListener != null) {
                mListener.onPushMessageTaskCallbackExecuted(mCreatedUidsMap);
            }
        }
    }

    /**
     * Push messages
     * 
     * @param messages
     * @return
     */
    public Boolean pushMessages(List<XmsDataObject> messages) {
        String from, to, direction;
        from = to = direction = null;

        try {
            BasicImapService imapService = mImapServiceController.getService();
            List<String> existingFolders = new ArrayList<String>();
            for (ImapFolder imapFolder : imapService.listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            for (XmsDataObject message : messages) {
                List<Flag> flags = new ArrayList<Flag>();
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
                    imapService.create(remoteFolder);
                    existingFolders.add(remoteFolder);
                }
                imapService.selectCondstore(remoteFolder);
                int uid = imapService.append(remoteFolder, flags, imapMessage.toPayload());
                mCreatedUidsMap.put(message.getMessageId(), uid);
            }
        } catch (IOException | ImapException | ImapServiceNotAvailableException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
                e.printStackTrace(); // FIX ME : debug purpose
            }
        }
        return true;
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
