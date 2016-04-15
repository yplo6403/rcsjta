/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.presence.pidf.Contact;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Terminating file transfer HTTP session starting from system resuming (because core was
 * restarted).
 */
public class FileTransferDownloadSession extends TerminatingHttpFileSharingSession implements FileTransferDownloadListener {

    private static final Logger sLogger = Logger
            .getLogger(FileTransferDownloadSession.class.getSimpleName());

    private final String mFiletransferId;
    /**
     * Constructor create instance of session object to resume download
     *
     * @param rcsSettings
     * @param messagingLog
     */

    public FileTransferDownloadSession(InstantMessagingService imService, FileTransferHttpInfoDocument fileTransferInfo,
                                       String fileTransferId, String chatId, ContactId contact, RcsSettings rcsSettings,
                                       MessagingLog messagingLog, long timestamp) {
        // @formatter:off
        super(imService,
                fileTransferInfo.getLocalMmContent(),
                fileTransferInfo.getExpiration(),
                null,
                -1,
                contact,
                chatId,
                fileTransferId,
                contact == null || !chatId.equals(contact.toString()),
                fileTransferInfo.getUri(),
                rcsSettings,
                messagingLog,
                timestamp,
                "",
                null);
        // @formatter:on
        mFiletransferId = fileTransferId;
        setSessionAccepted();
    }

    @Override
    public void run() {
        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Download a HTTP file transfer");
        }
        try {
            ContactId contact = getRemoteContact();
            onFileTransferDownloadStarted(contact);
            /* Download file from the HTTP server */
            mDownloadManager.downloadFile();
            if (logActivated) {
                sLogger.debug("Download success for ");
            }
            /* Set file URL */
            getContent().setUri(mDownloadManager.getDownloadedFileUri());
            onFileTransferDownloaded(contact);

        } catch (FileNotFoundException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            sLogger.error(new StringBuilder("Download file has failed for ").append(mFiletransferId)
                    .toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (FileNotDownloadedException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            sLogger.error(new StringBuilder("Download file has failed for ").append(mFiletransferId)
                    .toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (IOException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                return;
            }
            sLogger.error(new StringBuilder("Download file has failed for ").append(mFiletransferId)
                    .toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (NetworkException e) {
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error(new StringBuilder("Download file has failed for ").append(mFiletransferId)
                    .toString(), e);
            handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
        }
    }

    @Override
    public void onHttpTransferPausedBySystem() {
        onFileTransferDownloadPausedBySystem(getRemoteContact());
    }

    @Override
    public void onFileTransferDownloadStarted(ContactId contact) {
        for (ImsSessionListener listener : getListeners()) {
            ((FileTransferDownloadListener) listener).onFileTransferDownloadStarted(contact);
        }
    }

    @Override
    public void onFileTransferDownloadPausedBySystem(ContactId contact) {
        for (ImsSessionListener listener : getListeners()) {
            ((FileTransferDownloadListener) listener).onFileTransferDownloadPausedBySystem(contact);
        }
    }

    @Override
    public void onFileTransferDownloaded(ContactId contact) {
        for (ImsSessionListener listener : getListeners()) {
            ((FileTransferDownloadListener) listener).onFileTransferDownloaded(contact);
        }
    }
}
