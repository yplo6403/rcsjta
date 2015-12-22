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

import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;

import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;

/**
 * Task used to delete mailboxes or messages on the CMS server.
 * Used by the 'CMS Toolkit' or integration test
 */
public class DeleteTask implements Runnable {

    public enum Operation {
        DELETE_ALL, // delete all content for a user
        DELETE_MAILBOX, // delete one mailbox
        DELETE_MESSAGES // delete messages of a mailbox
    }

    private final ImapServiceController mImapServiceController;
    private final Operation mOperation;
    private final DeleteTaskListener mListener;
    private final String mMailbox;

    /**
     * Constructor
     * @param imapServiceController
     * @param operation
     * @param mailbox
     * @param listener
     * @throws ImapServiceNotAvailableException
     */
    public DeleteTask(ImapServiceController imapServiceController, Operation operation, String mailbox, DeleteTaskListener listener)
            throws ImapServiceNotAvailableException {
        mImapServiceController = imapServiceController;
        mOperation = operation;
        mMailbox = mailbox;
        mListener = listener;
    }

    @Override
    public void run() {
        boolean result = false;
        try {
            mImapServiceController.getService().init();
            result = delete(mMailbox);
        } catch (Exception e) {
        } finally {
            mImapServiceController.closeService();
            if (mListener != null) {
                mListener.onDeleteTaskExecuted(result);
            }
        }
    }

    /**
     *
     * @param mailbox
     * @return
     */
    public boolean delete(String mailbox) {

        try {
            switch (mOperation) {
                case DELETE_ALL:
                    deleteAll();
                    break;
                case DELETE_MAILBOX:
                    deleteMailbox(mailbox);
                    break;
                case DELETE_MESSAGES:
                    deleteMessages(mailbox);
                    break;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void deleteAll() throws IOException, ImapException, ImapServiceNotAvailableException {
        for (ImapFolder imapFolder : mImapServiceController.getService().listStatus()) {
            mImapServiceController.getService().delete(imapFolder.getName());
        }
    }

    private void deleteMailbox(String mailbox) throws IOException, ImapException, ImapServiceNotAvailableException {
        mImapServiceController.getService().delete(mailbox);
    }

    private void deleteMessages(String mailbox) throws IOException, ImapException, ImapServiceNotAvailableException {
        mImapServiceController.getService().select(mailbox);
        mImapServiceController.getService().expunge();
    }

    /**
     * Interface used to notify listeners of deletion when call in an asynchronous way
     */
    public interface DeleteTaskListener {
        /**
         * Callback method
         * @param result
         */
        void onDeleteTaskExecuted(Boolean result);
    }

}
