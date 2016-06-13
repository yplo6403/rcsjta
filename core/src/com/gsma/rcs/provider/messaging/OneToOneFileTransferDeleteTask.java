/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.DeliveryExpirationManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

public class OneToOneFileTransferDeleteTask extends DeleteTask.GroupedByContactId {

    private static final Logger sLogger = Logger.getLogger(OneToOneFileTransferDeleteTask.class
            .getName());

    private static final String SEL_ALL_ONETOONE_FILETRANSFERS = FileTransferData.KEY_CHAT_ID + "="
            + FileTransferData.KEY_CONTACT;

    private static final String SEL_ONETOONE_FILETRANSFER_BY_CHATID = FileTransferData.KEY_CHAT_ID
            + "=?";

    private final FileTransferServiceImpl mFileTransferService;

    private final InstantMessagingService mImService;
    private final CmsSessionController mCmsSessionCtrl;
    private final CmsLog mCmsLog;

    /**
     * Deletion of all one to one file transfers.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param cmsSessionCtrl the CMS session controller
     */
    public OneToOneFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            CmsSessionController cmsSessionCtrl) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID, true,
                FileTransferData.KEY_CONTACT, false, SEL_ALL_ONETOONE_FILETRANSFERS);
        mFileTransferService = fileTransferService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsLog = null;
    }

    /**
     * Constructor to process action of deletion for a specific file transfer ID
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param transferId the transfer id
     * @param cmsSessionCtrl the CMS session controller
     */
    public OneToOneFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            String transferId, CmsSessionController cmsSessionCtrl) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID, true,
                FileTransferData.KEY_CONTACT, true, null, transferId);
        mFileTransferService = fileTransferService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsLog = null;
    }

    /**
     * Constructor to process event of deletion for a specific file transfer ID
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param transferId the transfer id
     * @param cmsLog the CMS log accessor
     */
    public OneToOneFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            String transferId, CmsLog cmsLog) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID, true,
                FileTransferData.KEY_CONTACT, true, null, transferId);
        mFileTransferService = fileTransferService;
        mImService = imService;
        mCmsSessionCtrl = null;
        mCmsLog = cmsLog;
    }

    /**
     * Deletion of all file transfers from a specific one to one conversation.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param contact the contact id
     * @param cmsSessionCtrl the CMS session controller
     */
    public OneToOneFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            ContactId contact, CmsSessionController cmsSessionCtrl) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID, true,
                FileTransferData.KEY_CONTACT, false, SEL_ONETOONE_FILETRANSFER_BY_CHATID, contact
                        .toString());
        mFileTransferService = fileTransferService;
        mImService = imService;
        mCmsSessionCtrl = cmsSessionCtrl;
        mCmsLog = null;
    }

    @Override
    protected void onRowDelete(ContactId contact, String transferId) throws PayloadException {
        FileSharingSession session = mImService.getFileSharingSession(transferId);
        if (session == null) {
            mFileTransferService.ensureThumbnailIsDeleted(transferId);
            mFileTransferService.ensureFileCopyIsDeletedIfExisting(transferId);
            mFileTransferService.removeOneToOneFileTransfer(transferId);
            return;
        }
        try {
            session.deleteSession();
        } catch (NetworkException e) {
            /*
             * If network is lost during a delete operation the remaining part of the delete
             * operation (delete from persistent storage) can succeed to 100% anyway since delete
             * can be executed anyway while no network connectivity is present and still succeed.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
        mFileTransferService.ensureThumbnailIsDeleted(transferId);
        mFileTransferService.ensureFileCopyIsDeletedIfExisting(transferId);
        mFileTransferService.removeOneToOneFileTransfer(transferId);
    }

    @Override
    protected void onCompleted(ContactId contact, Set<String> transferIds) {
        DeliveryExpirationManager expirationManager = mImService.getDeliveryExpirationManager();
        for (String transferId : transferIds) {
            expirationManager.cancelDeliveryTimeoutAlarm(transferId);
        }
        if (mCmsSessionCtrl != null) {
            mCmsSessionCtrl.getFileTransferEventHandler()
                    .onDeleteFileTransfer(contact, transferIds);
        } else {
            for (String delId : transferIds) {
                mCmsLog.updateRcsDeleteStatus(CmsObject.MessageType.FILE_TRANSFER, delId,
                        CmsObject.DeleteStatus.DELETED, null);
            }
        }
        mFileTransferService.broadcastOneToOneFileTransferDeleted(contact, transferIds);
    }
}
