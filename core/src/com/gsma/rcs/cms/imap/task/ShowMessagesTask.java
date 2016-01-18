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

package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.ImapMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Task used to show messages from the CMS server. Used by the 'CMS Toolkit'
 */
public class ShowMessagesTask implements Runnable {

    private final ShowMessagesTaskListener mListener;
    private final ImapServiceController mImapServiceController;
    private static final Logger sLogger = Logger.getLogger(ShowMessagesTask.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param imapServiceController
     * @param listener
     * @throws ImapServiceNotAvailableException
     */
    public ShowMessagesTask(ImapServiceController imapServiceController,
            ShowMessagesTaskListener listener) throws ImapServiceNotAvailableException {
        mImapServiceController = imapServiceController;
        mListener = listener;
    }

    @Override
    public void run() {
        List<ImapMessage> messages = new ArrayList<>();
        try {
            mImapServiceController.initService();
            messages = getMessages(mImapServiceController.getService());

        } catch (ImapServiceNotAvailableException | NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.info("Failed to get messages!" + e.getMessage());
            }
        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Failed to get messages!", e);
        } finally {
            try {
                mImapServiceController.closeService();
            } catch (NetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.info("Failed to close CMS service! error=" + e.getMessage());
                }
            } catch (PayloadException | RuntimeException e) {
                sLogger.error("Failed to close CMS service", e);
            }
            if (mListener != null) {
                mListener.onShowMessagesTaskExecuted(messages);
            }
        }
    }

    private List<ImapMessage> getMessages(BasicImapService imap) {
        List<ImapMessage> messages = new ArrayList<>();
        try {
            for (ImapFolder imapFolder : imap.listStatus()) {
                imap.selectCondstore(imapFolder.getName());
                List<ImapMessage> imapMessages = imap.fetchAllMessages();
                for (ImapMessage imapMessage : imapMessages) {
                    imapMessage.setFolderPath(imapFolder.getName());
                    messages.add(imapMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * Interface used to notify listeners when messages can be shown (when call in an asynchronous
     * way)
     */
    public interface ShowMessagesTaskListener {
        /**
         * Callback method
         * 
         * @param result
         */
        void onShowMessagesTaskExecuted(List<ImapMessage> result);
    }
}
