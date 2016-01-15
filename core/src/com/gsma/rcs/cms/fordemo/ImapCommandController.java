
package com.gsma.rcs.cms.fordemo;

import com.gsma.rcs.cms.event.XmsMessageListener;
import com.gsma.rcs.cms.imap.service.ImapServiceController;
import com.gsma.rcs.cms.imap.task.PushMessageTask;
import com.gsma.rcs.cms.imap.task.PushMessageTask.PushMessageTaskListener;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask;
import com.gsma.rcs.cms.imap.task.UpdateFlagTask.UpdateFlagTaskListener;
import com.gsma.rcs.cms.observer.XmsObserverListener;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData.DeleteStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.sync.strategy.FlagChange;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.os.Handler;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class should be removed. Should be used for testing purpose only It creates SMS messages on
 * the CMS server with IMAP command.
 */
public class ImapCommandController implements XmsObserverListener, XmsMessageListener,
        PushMessageTaskListener, UpdateFlagTaskListener {

    private static final Logger sLogger = Logger.getLogger(ImapCommandController.class
            .getSimpleName());
    private final Context mContext;
    private final RcsSettings mSettings;
    private final XmsLog mXmsLog;
    private final ImapLog mImapLog;
    private final Handler mHandler;
    private final ImapServiceController mImapServiceController;

    public ImapCommandController(Handler handler, Context context, RcsSettings settings,
            ImapLog imapLog, XmsLog xmsLog, ImapServiceController imapServiceController) {
        mHandler = handler;
        mContext = context;
        mSettings = settings;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
        mImapServiceController = imapServiceController;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onIncomingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onIncomingSms");
        }
        if (!mImapServiceController.isSyncAvailable()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Imap sync not available");
                sLogger.debug(" --> PushMessageTask will not be started now");
            }
            return;
        }
        mHandler.post(new PushMessageTask(mContext, mSettings, mImapServiceController, mXmsLog,
                mImapLog, this));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onOutgoingSms(SmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onOutgoingSms");
        }
        if (!mImapServiceController.isSyncAvailable()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Imap sync not available");
                sLogger.debug(" --> PushMessageTask will not be started now");
            }
            return;
        }
        mHandler.post(new PushMessageTask(mContext, mSettings, mImapServiceController, mXmsLog,
                mImapLog, this));
    }

    @Override
    public void onDeleteSmsFromNativeApp(long nativeProviderId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onDeleteNativeSms");
        }

        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onIncomingMms");
        }
        if (!mImapServiceController.isSyncAvailable()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Imap sync not available");
                sLogger.debug(" --> PushMessageTask will not be started now");
            }
            return;
        }
        mHandler.post(new PushMessageTask(mContext, mSettings, mImapServiceController, mXmsLog,
                mImapLog, this));
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) {
        if (sLogger.isActivated()) {
            sLogger.info("onOutgoingMms");
        }
        if (!mImapServiceController.isSyncAvailable()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Imap sync not available");
                sLogger.debug(" --> PushMessageTask will not be started now");
            }
            return;
        }
        mHandler.post(new PushMessageTask(mContext, mSettings, mImapServiceController, mXmsLog,
                mImapLog, this));
    }

    @Override
    public void onDeleteMmsFromNativeApp(String mmsId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onDeleteNativeMms");
        }

        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onXmsMessageStateChanged(Long nativeProviderId, String mimeType, State state) {
    }

    @Override
    public void onReadXmsConversationFromNativeApp(long nativeThreadId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadNativeConversation");
        }

        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteXmsConversationFromNativeApp(long nativeThreadId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onDeleteNativeConversation : " + nativeThreadId);
        }

        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @SuppressWarnings("unchecked")
    private void updateFlags() {
        if (!mImapServiceController.isSyncAvailable()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Imap sync not available");
                sLogger.debug(" --> UpdateFlagTask will not be started now");
            }
            return;
        }
        mHandler.post(new UpdateFlagTask(mImapServiceController, mImapLog, this));
    }

    @Override
    public void onReadXmsMessage(String messageId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadXmsMessage : " + messageId);
        }
        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onReadXmsConversation(ContactId contactId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadXmsConversation : ".concat(contactId.toString()));
        }

        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteXmsMessage(String messageId) {
        boolean isLogActivated = sLogger.isActivated();
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteXmsMessage :" + messageId);
        }
        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onDeleteXmsConversation(ContactId contactId) {
        boolean isLogActivated = sLogger.isActivated();
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteXmsConversation :".concat(contactId.toString()));
        }
        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onXmsMessageStateChanged(ContactId contact, String messageId, String mimeType,
            State state) {

    }

    @Override
    public void onDeleteAllXmsMessage() {
        boolean isLogActivated = sLogger.isActivated();
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteAllXmsMessage");
        }
        if (!mSettings.getCmsUpdateFlagsWithImapXms()) {
            if (isLogActivated) {
                sLogger.info("Update flags with IMAP command not enabled for XMS");
            }
            return;
        }
        updateFlags();
    }

    @Override
    public void onPushMessageTaskCallbackExecuted(Map<String, Integer> uidsMap) {
        if (sLogger.isActivated()) {
            sLogger.info("onPushMessageTaskCallbackExecuted");
        }
        for (Entry<String, Integer> entry : uidsMap.entrySet()) {
            String baseId = entry.getKey();
            Integer uid = entry.getValue();
            mImapLog.updateXmsPushStatus(uid, baseId, PushStatus.PUSHED);
        }
    }

    @Override
    public void onUpdateFlagTaskExecuted(List<FlagChange> flags) {
        if (sLogger.isActivated()) {
            sLogger.info("onUpdateFlagTaskExecuted");
        }
        for (FlagChange fg : flags) {
            boolean deleted = fg.isDeleted();
            boolean seen = fg.isSeen();
            String folderName = fg.getFolder();
            for (Integer uid : fg.getUids()) {
                if (deleted) {
                    mImapLog.updateDeleteStatus(folderName, uid, DeleteStatus.DELETED);
                } else if (seen) {
                    mImapLog.updateReadStatus(folderName, uid, ReadStatus.READ);
                }
            }
        }
    }

}
