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

import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.service.ImapServiceNotAvailableException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;

/**
 * Task used to delete mailboxes or messages on the CMS server. Used by the 'CMS Toolkit' or
 * integration test
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
    private static final Logger sLogger = Logger.getLogger(DeleteTask.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param imapServiceController the IMAP service controller
     * @param operation the operation
     * @param mailbox the mailbox
     * @param listener the listener
     * @throws ImapServiceNotAvailableException
     */
    public DeleteTask(ImapServiceController imapServiceController, Operation operation,
            String mailbox, DeleteTaskListener listener) throws ImapServiceNotAvailableException {
        mImapServiceController = imapServiceController;
        mOperation = operation;
        mMailbox = mailbox;
        mListener = listener;
    }

    @Override
    public void run() {
        boolean result = false;
        try {
            mImapServiceController.createService().init();
            delete(mMailbox);
            result = true;

        } catch (ImapServiceNotAvailableException | IOException | NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.info("Failed to delete mailbox: '" + mMailbox + "'! error="
                        + e.getMessage());
            }
        } catch (ImapException | PayloadException | RuntimeException e) {
            sLogger.error("Failed to delete mailbox: '" + mMailbox + "'!", e);

        } finally {
            try {
                mImapServiceController.closeService();
            } catch (NetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.info("Failed to close CMS service! error=" + e.getMessage());
                }
            } catch (PayloadException e) {
                sLogger.error("Failed to close CMS service", e);
            }
            if (mListener != null) {
                mListener.onDeleteTaskExecuted(result);
            }
        }
    }

    /**
     * Deletes the mailbox
     * 
     * @param mailbox the mailbox
     */
    public void delete(String mailbox) throws NetworkException, PayloadException,
            ImapServiceNotAvailableException {

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

        } catch (IOException e) {
            throw new NetworkException("Failed to delete mailbox " + mailbox, e);

        } catch (ImapException e) {
            throw new PayloadException("Failed to delete mailbox " + mailbox, e);
        }
    }

    private void deleteAll() throws IOException, ImapException, ImapServiceNotAvailableException {
        for (String imapFolder : mImapServiceController.getService().list()) {
            mImapServiceController.getService().delete(imapFolder);
        }
    }

    private void deleteMailbox(String mailbox) throws IOException, ImapException,
            ImapServiceNotAvailableException {
        mImapServiceController.getService().delete(mailbox);
    }

    private void deleteMessages(String mailbox) throws IOException, ImapException,
            ImapServiceNotAvailableException {
        mImapServiceController.getService().select(mailbox);
        mImapServiceController.getService().expunge();
    }

    /**
     * Interface used to notify listeners of deletion when call in an asynchronous way
     */
    public interface DeleteTaskListener {
        /**
         * Callback method
         * 
         * @param result True is delete is successful
         */
        void onDeleteTaskExecuted(Boolean result);
    }

}
