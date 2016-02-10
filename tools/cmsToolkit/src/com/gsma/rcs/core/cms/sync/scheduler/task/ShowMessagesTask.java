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

import com.gsma.rcs.core.cms.protocol.cmd.ImapFolder;
import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.sync.scheduler.SchedulerTask;

import com.gsma.rcs.imaplib.imap.ImapMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Task used to show messages from the CMS server. Used by the 'CMS Toolkit'
 */
public class ShowMessagesTask extends SchedulerTask {

    private final ShowMessagesTaskListener mListener;
    private static final Logger sLogger = Logger.getLogger(ShowMessagesTask.class.getSimpleName());

    /**
     * Constructor
     *
     * @param listener
     */
    public ShowMessagesTask(ShowMessagesTaskListener listener) {
        mListener = listener;
    }

    @Override
    public void run() {
        List<ImapMessage> messages = new ArrayList<>();
        try {
            messages = getMessages(getBasicImapService());
        } catch (RuntimeException e) {
            e.printStackTrace();
            sLogger.severe("Failed to get messages!");
        } finally {
            if (mListener != null) {
                mListener.onShowMessagesTaskExecuted(messages);
            }
            try {
                getBasicImapService().close();
            } catch (IOException e) {
                e.printStackTrace();
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
