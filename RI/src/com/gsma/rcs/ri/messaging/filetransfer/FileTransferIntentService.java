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
 ******************************************************************************/

package com.gsma.rcs.ri.messaging.filetransfer;

import static com.gsma.rcs.ri.utils.FileUtils.takePersistableContentUriPermission;

import com.gsma.rcs.api.connection.ConnectionManager;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.messaging.OneToOneTalkView;
import com.gsma.rcs.ri.messaging.TalkList;
import com.gsma.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.gsma.rcs.ri.settings.RiSettings;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.CommonServiceConfiguration;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * File transfer intent service
 * 
 * @author Philippe LEMORDANT
 */
public class FileTransferIntentService extends IntentService {

    private static final String LOGTAG = LogUtils.getTag(FileTransferIntentService.class.getName());

    private final static String[] PROJ_UNDELIVERED_FT = new String[] {
        FileTransferLog.FT_ID
    };

    private static final String SEL_UNDELIVERED_FTS = FileTransferLog.CHAT_ID + "=? AND "
            + FileTransferLog.EXPIRED_DELIVERY + "='1'";

    /**
     * Constructor
     */
    public FileTransferIntentService() {
        super("FileTransferIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /*
         * We want this service to stop running if forced stop so return not sticky.
         */
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action;
        if ((action = intent.getAction()) == null) {
            return;
        }
        String transferId = intent.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
        if (transferId == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read transfer ID");
            }
            return;
        }
        /* Get File Transfer from provider */
        FileTransferDAO ftDao = FileTransferDAO.getFileTransferDAO(this, transferId);
        if (ftDao == null) {
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onHandleIntent file transfer with ID ".concat(transferId));
        }
        switch (action) {
            case FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED:
                handleUndeliveredFileTransfer(intent, transferId);
                break;
            case FileTransferIntent.ACTION_NEW_INVITATION:
                handleFileTransferInvitation(intent, ftDao);
                break;
            case FileTransferIntent.ACTION_RESUME:
                handleFileTransferResume(intent, ftDao);
                break;
            default:
                Log.e(LOGTAG, "Unknown action ".concat(action));
        }
    }

    private void handleFileTransferResume(Intent intent, FileTransferDAO ftDao) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onHandleIntent file transfer resume with ID ".concat(ftDao.getChatId()));
        }
        if (Direction.INCOMING == ftDao.getDirection()) {
            startActivity(ReceiveFileTransfer.forgeResumeIntent(this, ftDao, intent));
        } else {
            startActivity(InitiateFileTransfer.forgeResumeIntent(this, ftDao, intent));
        }
    }

    private void handleFileTransferInvitation(Intent intent, FileTransferDAO ftDao) {
        /* Check if file transfer is already rejected */
        if (FileTransfer.State.REJECTED == ftDao.getState()) {
            Log.e(LOGTAG, "File transfer already rejected. Id=".concat(ftDao.getChatId()));
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "File Transfer invitation filename=" + ftDao.getFilename() + " size="
                    + ftDao.getSize());
        }
        forwardFileTransferInvitationToUi(intent, ftDao);
    }

    /**
     * Forward file transfer invitation to UI
     *
     * @param invitation Intent invitation
     * @param ftDao the file transfer data object
     */
    private void forwardFileTransferInvitationToUi(Intent invitation, FileTransferDAO ftDao) {
        ContactId contact = ftDao.getContact();
        if (ftDao.getContact() == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "forwardFileTransferInvitationToUi failed: cannot parse contact");
            }
            return;
        }
        Intent intent = ReceiveFileTransfer.forgeInvitationIntent(this, ftDao, invitation);
        /*
         * If the PendingIntent has the same operation, action, data, categories, components, and
         * flags it will be replaced. Invitation should be notified individually so we use a random
         * generator to provide a unique request code and reuse it for the notification.
         */
        int uniqueId = Utils.getUniqueIdForPendingIntent();
        PendingIntent pi = PendingIntent.getActivity(this, uniqueId, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
        String title = getString(R.string.title_recv_file_transfer);
        String message = getString(R.string.label_from_args, displayName);

        /* Send notification */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = buildNotification(pi, title, message);
        notificationManager.notify(uniqueId, notif);
        TalkList.notifyNewConversationEvent(this, FileTransferIntent.ACTION_NEW_INVITATION);
    }

    private void handleUndeliveredFileTransfer(Intent intent, String transferId) {
        ContactId contact = intent.getParcelableExtra(FileTransferIntent.EXTRA_CONTACT);
        if (contact == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read contact for ftId=".concat(transferId));
            }
            return;
        }
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Undelivered file transfer ID=" + transferId + " for contact " + contact);
        }
        RiSettings.PreferenceResendRcs preferenceResendFt = RiSettings
                .getPreferenceResendFileTransfer(this);
        switch (preferenceResendFt) {
            case DO_NOTHING:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Undelivered ID=" + transferId + " for contact " + contact
                            + ": do nothing");
                }
                clearExpiredDeliveryFileTransfers(contact);
                break;

            case SEND_TO_XMS:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Undelivered ID=" + transferId + " for contact " + contact
                            + ": send to SMS");
                }
                resendFileTransfersViaMms(contact);
                break;

            case ALWAYS_ASK:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Undelivered ID=" + transferId + " for contact " + contact
                            + ": ask user");
                }
                forwardUndeliveredFileTransferToUi(intent, contact);
        }
    }

    private void forwardUndeliveredFileTransferToUi(Intent undeliveredIntent, ContactId contact) {
        Intent intent = OneToOneTalkView.forgeIntentOnStackEvent(this, contact, undeliveredIntent);
        ChatPendingIntentManager pendingIntentmanager = ChatPendingIntentManager
                .getChatPendingIntentManager(this);
        Integer uniqueId = pendingIntentmanager.tryContinueChatConversation(intent,
                contact.toString());
        if (uniqueId != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(contact);
            String title = getString(R.string.title_undelivered_filetransfer);
            String msg = getString(R.string.label_undelivered_filetransfer, displayName);
            Notification notif = buildNotification(contentIntent, title, msg);
            pendingIntentmanager.postNotification(uniqueId, notif);
        }
    }

    /**
     * Generate a notification
     *
     * @param pendingIntent pending intent
     * @param title title
     * @param message message
     * @return the notification
     */
    private Notification buildNotification(PendingIntent pendingIntent, String title, String message) {
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(pendingIntent);
        notif.setSmallIcon(R.drawable.ri_notif_file_transfer_icon);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(true);
        notif.setOnlyAlertOnce(true);
        notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        notif.setDefaults(Notification.DEFAULT_VIBRATE);
        notif.setContentTitle(title);
        notif.setContentText(message);
        return notif.build();
    }

    private void resendFileTransfersViaMms(ContactId contact) {
        Set<String> transferIds = getUndelivered(this, contact);
        if (transferIds.isEmpty()) {
            return;
        }
        ConnectionManager cnxManager = ConnectionManager.getInstance();
        if (!cnxManager.isServiceConnected(ConnectionManager.RcsServiceName.FILE_TRANSFER,
                ConnectionManager.RcsServiceName.CMS)) {
            return;
        }
        FileTransferService fileTransferService = cnxManager.getFileTransferApi();
        CmsService cmsService = cnxManager.getCmsApi();
        try {
            for (String id : transferIds) {
                FileTransfer fileTransfer = fileTransferService.getFileTransfer(id);
                if (fileTransfer == null) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "Cannot resend via MMS transfer ID=" + id + ": not found!");
                    }
                    continue;
                }
                List<Uri> files = new ArrayList<>();
                Uri file = fileTransfer.getFile();
                if (!Utils.isImageType(fileTransfer.getMimeType())) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "Cannot resend via MMS transfer ID=" + id + ": not image!");
                    }
                    continue;
                }
                takePersistableContentUriPermission(this, file);
                files.add(file);
                final String subject = getString(R.string.switch_mms_subject,
                        getMyDisplayName(fileTransferService));
                final String body = getString(R.string.switch_mms_body);
                cmsService.sendMultimediaMessage(contact, files, subject, body);
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Re-send via MMS transfer ID=".concat(id));
                }
                fileTransferService.deleteFileTransfer(id);
            }
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    private void clearExpiredDeliveryFileTransfers(ContactId contact) {
        Set<String> msgIds = getUndelivered(this, contact);
        if (msgIds.isEmpty()) {
            return;
        }
        ConnectionManager cnxManager = ConnectionManager.getInstance();
        if (!cnxManager.isServiceConnected(ConnectionManager.RcsServiceName.FILE_TRANSFER)) {
            return;
        }
        FileTransferService fileTransferService = cnxManager.getFileTransferApi();
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Clear delivery expiration for IDs=" + msgIds);
            }
            fileTransferService.clearFileTransferDeliveryExpiration(msgIds);

        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    /**
     * Get set of undelivered file transfers
     * 
     * @param ctx The context
     * @param contact The contact
     * @return set of undelivered file transfers
     */
    public static Set<String> getUndelivered(Context ctx, ContactId contact) {
        Set<String> ids = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(FileTransferLog.CONTENT_URI,
                    PROJ_UNDELIVERED_FT, SEL_UNDELIVERED_FTS, new String[] {
                        contact.toString()
                    }, null);
            if (cursor == null) {
                throw new SQLException("Cannot query undelivered file transfers for contact="
                        + contact);
            }
            if (!cursor.moveToFirst()) {
                return ids;
            }
            int idColumnIdx = cursor.getColumnIndexOrThrow(FileTransferLog.FT_ID);
            do {
                ids.add(cursor.getString(idColumnIdx));
            } while (cursor.moveToNext());
            return ids;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getMyDisplayName(FileTransferService fileTransferService)
            throws RcsGenericException, RcsServiceNotAvailableException {
        CommonServiceConfiguration config = fileTransferService.getCommonConfiguration();
        String myDisplayName = config.getMyDisplayName();
        if (myDisplayName == null) {
            myDisplayName = config.getMyContactId().toString();
        }
        return myDisplayName;
    }
}
