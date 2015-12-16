// TODO add copyright

package com.gsma.rcs.cms;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.event.IRcsXmsEventListener;
import com.gsma.rcs.cms.event.XmsEventListener;
import com.gsma.rcs.cms.fordemo.ImapCommandController;
import com.gsma.rcs.cms.observer.XmsObserver;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.imap.MessageData.PushStatus;
import com.gsma.rcs.cms.provider.imap.MessageData.ReadStatus;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.service.cms.mms.MmsSessionListener;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.service.broadcaster.XmsMessageEventBroadcaster;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessage.ReasonCode;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

public class CmsManager implements IRcsXmsEventListener, MmsSessionListener {

    private Context mContext;
    private XmsObserver mXmsObserver;
    private XmsEventListener mXmsEventListener;
    private LocalStorage mLocalStorage;
    private ImapLog mImapLog;
    private XmsLog mXmsLog;
    private RcsSettings mRcsSettings;
    private ImapCommandController mImapCommandController;

    /**
     * Constructor of CmsManager
     *
     * @param context The context
     * @param imapLog The IMAP log accessor
     * @param xmsLog The XMS log accessor
     * @param rcsSettings THE RCS settings accessor
     */
    public CmsManager(Context context, ImapLog imapLog, XmsLog xmsLog, RcsSettings rcsSettings) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
    }

    public void start() {
        // execute sync between providers with async task
        new ProviderSynchronizer(mContext.getContentResolver(), mRcsSettings, mXmsLog, mImapLog)
                .execute();

        // instantiate Xms Observer on native SMS/MMS content provider
        mXmsObserver = new XmsObserver(mContext);

        // instantiate XmsEventListener in charge of handling xms events from XmsObserver
        mXmsEventListener = new XmsEventListener(mContext, mImapLog, mXmsLog, mRcsSettings);
        mXmsObserver.registerListener(mXmsEventListener);

        // instantiate LocalStorage in charge of handling events relatives to IMAP sync
        mLocalStorage = new LocalStorage(mImapLog);
        mLocalStorage.registerRemoteEventHandler(MessageType.SMS, mXmsEventListener);
        mLocalStorage.registerRemoteEventHandler(MessageType.MMS, mXmsEventListener);
        // mLocalStorage.registerRemoteEventHandler(MessageType.ONETOONE, tobedefined);
        // mLocalStorage.registerRemoteEventHandler(MessageType.GC, tobedefined);

        // instantiate ImapCommandController in charge of Pushing messages and updating flags with
        // Imap command
        mImapCommandController = new ImapCommandController(mContext, mRcsSettings, mLocalStorage,
                mImapLog, mXmsLog);
        mXmsObserver.registerListener(mImapCommandController);

        // start content observer on native SMS/MMS content provider
        mXmsObserver.start();
    }

    public void stop() {
        if (mXmsObserver != null) {
            mXmsObserver.stop();
            mXmsObserver = null;
        }
        // TODO FGI : Fix me : when the core is stopped while a background task is still processing
        // (IMAP sync)
        // if(mLocalStorage!=null){
        // mLocalStorage.removeListeners();
        // mLocalStorage = null;
        // }
        // mXmsEventListener = null;
        // mImapCommandController = null;
    }

    /**
     * @param listener The listener
     */
    public void registerSmsObserverListener(INativeXmsEventListener listener) {
        if (mXmsObserver != null) {
            mXmsObserver.registerListener(listener);
        }
    }

    /**
     * @param listener The listener
     */
    public void unregisterSmsObserverListener(INativeXmsEventListener listener) {
        if (mXmsObserver != null) {
            mXmsObserver.unregisterListener(listener);
        }
    }

    @Override
    public void onReadRcsMessage(String messageId) {
        if (mXmsEventListener != null) {
            mXmsEventListener.onReadRcsMessage(messageId);
        }
        if (mImapCommandController != null) {
            mImapCommandController.onReadRcsMessage(messageId);
        }
    }

    @Override
    public void onDeleteRcsMessage(String messageId) {
        if (mXmsEventListener != null) {
            mXmsEventListener.onDeleteRcsMessage(messageId);
        }
        if (mImapCommandController != null) {
            mImapCommandController.onDeleteRcsMessage(messageId);
        }
    }

    @Override
    public void onReadRcsConversation(ContactId contact) {
        if (mXmsEventListener != null) {
            mXmsEventListener.onReadRcsConversation(contact);
        }
        if (mImapCommandController != null) {
            mImapCommandController.onReadRcsConversation(contact);
        }
    }

    @Override
    public void onDeleteRcsConversation(ContactId contact) {
        if (mXmsEventListener != null) {
            mXmsEventListener.onDeleteRcsConversation(contact);
        }
        if (mImapCommandController != null) {
            mImapCommandController.onDeleteRcsConversation(contact);
        }
    }

    @Override
    public void onMessageStateChanged(ContactId contact, String messageId, String mimeType,
            State state) {
        if (mXmsEventListener != null) {
            mXmsEventListener.onMessageStateChanged(contact, messageId, mimeType, state);
        }
    }

    @Override
    public void onDeleteAll() {
        if (mXmsEventListener != null) {
            mXmsEventListener.onDeleteAll();
        }
        if (mImapCommandController != null) {
            mImapCommandController.onDeleteAll();
        }
    }

    public void registerXmsMessageEventBroadcaster(
            XmsMessageEventBroadcaster xmsMessageEventBroadcaster) {
        if (mXmsEventListener != null) {
            mXmsEventListener.registerBroadcaster(xmsMessageEventBroadcaster);
        }
    }

    public void unregisterXmsMessageEventBroadcaster(
            XmsMessageEventBroadcaster xmsMessageEventBroadcaster) {
        if (mXmsEventListener != null) {
            mXmsEventListener.unregisterBroadcaster(xmsMessageEventBroadcaster);
        }
    }

    public Context getContext() {
        return mContext;
    }

    public XmsLog getXmsLog() {
        return mXmsLog;
    }

    public LocalStorage getLocalStorage() {
        return mLocalStorage;
    }

    @Override
    public void onMmsTransferError(ReasonCode reason, ContactId contact, String mmsId) {
    }

    @Override
    public void onMmsTransferred(ContactId contact, String mmsId) {

        mImapLog.addMessage(new MessageData(CmsUtils.contactToCmsFolder(mRcsSettings, contact),
                ReadStatus.READ, MessageData.DeleteStatus.NOT_DELETED,
                mRcsSettings.getCmsPushSms() ? PushStatus.PUSH_REQUESTED : PushStatus.PUSHED,
                MessageType.MMS, mmsId, null));

        if (mImapCommandController != null) {
            MmsDataObject mms = (MmsDataObject) mXmsLog.getXmsDataObject(mmsId);
            if (Direction.INCOMING == mms.getDirection()) {
                mImapCommandController.onIncomingMms(mms);
            } else {
                mImapCommandController.onOutgoingMms(mms);
            }
        }
    }

    @Override
    public void onMmsTransferStarted(ContactId contact, String mmsId) {
    }
}
