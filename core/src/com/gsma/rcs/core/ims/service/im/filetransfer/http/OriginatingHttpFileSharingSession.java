/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.io.IOException;

/**
 * Originating file transfer HTTP session
 * 
 * @author vfml3370
 */
public class OriginatingHttpFileSharingSession extends HttpFileTransferSession implements
        HttpUploadTransferEventListener {

    private final InstantMessagingService mImService;

    protected HttpUploadManager mUploadManager;

    private static final Logger sLogger = Logger.getLogger(OriginatingHttpFileSharingSession.class
            .getName());

    /**
     * The timestamp to be sent in payload when the file sharing was initiated for outgoing file
     * sharing
     */
    private long mTimestampSent;

    /**
     * Constructor
     * 
     * @param fileTransferId File transfer Id
     * @param imService InstantMessagingService
     * @param content Content of file to share
     * @param contact Remote contact identifier
     * @param fileIcon Content of fileicon
     * @param tId TID of the upload
     * @param core Core
     * @param messagingLog MessagingLog
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param timestampSent the timestamp sent in payload for the file sharing
     * @param contactManager
     */
    public OriginatingHttpFileSharingSession(InstantMessagingService imService,
            String fileTransferId, MmContent content, ContactId contact, MmContent fileIcon,
            String tId, MessagingLog messagingLog, RcsSettings rcsSettings, long timestamp,
            long timestampSent, ContactManager contactManager) {
        // @formatter:off
        super(imService, 
                content,
                contact,
                PhoneUtils.formatContactIdToUri(contact),
                fileIcon,
                null,
                fileTransferId,
                rcsSettings,
                messagingLog,
                timestamp,
                FileTransferData.UNKNOWN_EXPIRATION,
                FileTransferData.UNKNOWN_EXPIRATION,
                contactManager);
        // @formatter:ofn
        mImService = imService;
        mTimestampSent = timestampSent;
        if (sLogger.isActivated()) {
            sLogger.debug("OriginatingHttpFileSharingSession contact=".concat(contact.toString()));
        }
        mUploadManager = new HttpUploadManager(getContent(), fileIcon, this, tId, rcsSettings);
    }

    @Override
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new HTTP file transfer session as originating");
            }
            // Upload the file to the HTTP server
            byte[] result = mUploadManager.uploadFile();
            sendResultToContact(result);

        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (IOException e) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mUploadManager.isCancelled() || mUploadManager.isPaused()) {
                return;
            }
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (PayloadException e) {
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));
        }
    }

    protected void sendResultToContact(byte[] result) throws PayloadException, NetworkException {
        // Check if upload has been cancelled
        if (mUploadManager.isCancelled()) {
            return;
        }
        boolean logActivated = sLogger.isActivated();
        FileTransferHttpInfoDocument infoDocument;
        if (result == null
                || (infoDocument = FileTransferUtils.parseFileTransferHttpDocument(result,
                        mRcsSettings)) == null) {
            /* Don't call handleError in case of Pause or Cancel */
            if (mUploadManager.isCancelled() || mUploadManager.isPaused()) {
                return;
            }
            handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED));
            return;

        }
        String fileInfo = new String(result, UTF8);
        if (logActivated) {
            sLogger.debug("Upload done with success: ".concat(fileInfo));
        }

        setFileExpiration(infoDocument.getExpiration());
        FileTransferHttpThumbnail thumbnail = infoDocument.getFileThumbnail();
        if (thumbnail != null) {
            setIconExpiration(thumbnail.getExpiration());
        } else {
            setIconExpiration(FileTransferData.UNKNOWN_EXPIRATION);
        }

        OneToOneChatSession chatSession = mImService.getOneToOneChatSession(getRemoteContact());
        // Note: FileTransferId is always generated to equal the associated msgId of a
        // FileTransfer invitation message.
        String msgId = getFileTransferId();
        if (chatSession != null && chatSession.isMediaEstablished()) {
            if (logActivated) {
                sLogger.debug("Send file transfer info via an existing chat session");
            }
            setContributionID(chatSession.getContributionID());

            String networkContent;

            if (mImdnManager.isRequestOneToOneDeliveryDisplayedReportsEnabled()) {
                networkContent = ChatUtils.buildCpimMessageWithImdn(ChatUtils.ANONYMOUS_URI,
                        ChatUtils.ANONYMOUS_URI, msgId, fileInfo,
                        FileTransferHttpInfoDocument.MIME_TYPE, mTimestampSent);
            } else if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
                networkContent = ChatUtils.buildCpimMessageWithoutDisplayedImdn(
                        ChatUtils.ANONYMOUS_URI, ChatUtils.ANONYMOUS_URI, msgId, fileInfo,
                        FileTransferHttpInfoDocument.MIME_TYPE, mTimestampSent);
            } else {
                networkContent = ChatUtils.buildCpimMessage(ChatUtils.ANONYMOUS_URI,
                        ChatUtils.ANONYMOUS_URI, fileInfo, FileTransferHttpInfoDocument.MIME_TYPE,
                        mTimestampSent);
            }
            chatSession.sendDataChunks(IdGenerator.generateMessageID(), networkContent,
                    CpimMessage.MIME_TYPE, MsrpSession.TypeMsrpChunk.HttpFileSharing);
        } else {
            if (logActivated) {
                sLogger.debug("Send file transfer info via a new chat session.");
            }
            long timestamp = getTimestamp();
            ChatMessage firstMsg = ChatUtils.createFileTransferMessage(getRemoteContact(),
                    fileInfo, msgId, timestamp, mTimestampSent);
            if (!mImService.isChatSessionAvailable()) {
                if (logActivated) {
                    sLogger.debug("Couldn't initiate One to one session as max chat sessions reached.");
                }
                mMessagingLog.setFileTransferDownloadInfo(getFileTransferId(), infoDocument);
                removeSession();
                return;
            }
            chatSession = mImService.createOneToOneChatSession(getRemoteContact(), firstMsg);
            setContributionID(chatSession.getContributionID());

            chatSession.startSession();
            mImService.receiveOneOneChatSessionInitiation(chatSession);
        }
        handleFileTransferred();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        mUploadManager.interrupt();
    }

    @Override
    public void onPause() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    fileTransferPaused();
                    interruptSession();
                    mUploadManager.pauseTransferByUser();

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED, e));
                }
            }
        }).start();
    }

    @Override
    public void onResume() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    fileTransferResumed();
                    FtHttpResumeUpload upload = mMessagingLog
                            .retrieveFtHttpResumeUpload(mUploadManager.getTId());
                    if (upload != null) {
                        sendResultToContact(mUploadManager.resumeUpload());
                    } else {
                        sendResultToContact(null);
                    }

                } catch (NetworkException e) {
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED, e));

                } catch (IOException e) {
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED, e));

                } catch (PayloadException e) {
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED, e));

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    handleError(new FileSharingError(FileSharingError.MEDIA_UPLOAD_FAILED, e));
                }
            }
        }).start();
    }

    @Override
    public void uploadStarted() {
        mMessagingLog.setFileUploadTId(getFileTransferId(), mUploadManager.getTId());
    }

    /**
     * Gets upload manager
     * 
     * @return upload manager
     */
    public HttpUploadManager getUploadManager() {
        return mUploadManager;
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    /**
     * Sets the timestamp when file icon on the content server is no longer valid to download.
     * 
     * @param timestamp
     */
    public void setIconExpiration(long timestamp) {
        mIconExpiration = timestamp;
    }

    /**
     * Sets the timestamp when file on the content server is no longer valid to download.
     * 
     * @param timestamp
     */
    public void setFileExpiration(long timestamp) {
        mFileExpiration = timestamp;
    }

    @Override
    public void terminateSession(TerminationReason reason) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("terminateSession reason=".concat(reason.toString()));
        }
        closeHttpSession(reason);
        /*
         * If reason is TERMINATION_BY_SYSTEM or TERMINATION_BY_CONNECTION_LOST and session already
         * started, then it's a pause
         */
        ContactId contact = getRemoteContact();
        State state = getSessionState();
        switch (reason) {
            case TERMINATION_BY_SYSTEM:
                /* Intentional fall through */
            case TERMINATION_BY_CONNECTION_LOST:
                if (isFileTransferPaused()) {
                    return;
                }
                /*
                 * TId id needed for resuming the file transfer. Hence pausing the file transfer
                 * only if TId is present.
                 */
                if (State.ESTABLISHED == state && mUploadManager.getTId() != null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Pause the session (session terminated, but can be resumed)");
                    }
                    for (ImsSessionListener listener : getListeners()) {
                        ((FileSharingSessionListener) listener)
                                .onFileTransferPausedBySystem(contact);
                    }
                    return;
                }
                /* Intentional fall through */
                //$FALL-THROUGH$
            default:
                if (State.ESTABLISHED == state) {
                    for (ImsSessionListener listener : getListeners()) {
                        listener.onSessionAborted(contact, reason);
                    }
                } else {
                    for (ImsSessionListener listener : getListeners()) {
                        listener.onSessionRejected(contact, reason);
                    }
                }
                break;
        }
    }
}
