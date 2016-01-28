
package com.gsma.rcs.cms.event.framework;

import com.gsma.rcs.cms.event.ChatMessageListener;
import com.gsma.rcs.cms.event.XmsMessageListener;
import com.gsma.rcs.cms.observer.XmsObserverListener;
import com.gsma.rcs.cms.scheduler.CmsScheduler;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventFrameworkMode;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

/**
 * This class is the entry point for the event framework. It allows: - to push XMS messages on the
 * message store with IMAP commands - to update flags of messages on the message store with SIP or
 * IMAP commands The protocol used for updating flags depends on provisioning parameters.
 */
public class EventFrameworkHandler implements XmsObserverListener, XmsMessageListener,
        ChatMessageListener {

    private static final Logger sLogger = Logger.getLogger(EventFrameworkHandler.class
            .getSimpleName());

    private final Context mContext;
    private final RcsSettings mSettings;
    private final ImapEventFrameworkHandler mImapEventFrameworkHandler;
    private final SipEventFrameworkHandler mSipEventFrameworkHandler;

    public EventFrameworkHandler(Context context, CmsScheduler scheduler, RcsSettings settings) {
        mContext = context;
        mImapEventFrameworkHandler = new ImapEventFrameworkHandler(context, scheduler, settings);
        mSipEventFrameworkHandler = new SipEventFrameworkHandler(context, settings);
        mSettings = settings;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onIncomingSms(SmsDataObject message) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("onIncomingSms");
        }
        if (mSettings.getMessageStorePushSms()) {
            mImapEventFrameworkHandler.onNewXmsMessage(message.getContact());
        } else {
            if (logActivated) {
                sLogger.info("Sms push is not allowed from settings");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onOutgoingSms(SmsDataObject message) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("onOutgoingSms");
        }
        if (mSettings.getMessageStorePushSms()) {
            mImapEventFrameworkHandler.onNewXmsMessage(message.getContact());
        } else {
            if (logActivated) {
                sLogger.info("Sms push is not allowed from settings");
            }
        }
    }

    @Override
    public void onIncomingMms(MmsDataObject message) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("onIncomingMms");
        }
        if (mSettings.getMessageStorePushMms()) {
            mImapEventFrameworkHandler.onNewXmsMessage(message.getContact());
        } else {
            if (logActivated) {
                sLogger.info("Mms push is not allowed from settings");
            }
        }
    }

    @Override
    public void onOutgoingMms(MmsDataObject message) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("onOutgoingMms");
        }
        if (mSettings.getMessageStorePushMms()) {
            mImapEventFrameworkHandler.onNewXmsMessage(message.getContact());
        } else {
            if (logActivated) {
                sLogger.info("Mms push is not allowed from settings");
            }
        }
    }

    @Override
    public void onDeleteSmsFromNativeApp(long nativeProviderId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onDeleteNativeSms");
        }
        updateXmsFlags();
    }

    @Override
    public void onDeleteMmsFromNativeApp(String mmsId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeMms");
        }
        updateXmsFlags();
    }

    @Override
    public void onXmsMessageStateChanged(Long nativeProviderId, String mimeType, State state) {
    }

    @Override
    public void onReadXmsConversationFromNativeApp(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.info("onReadNativeConversation");
        }
        updateXmsFlags();
    }

    @Override
    public void onDeleteXmsConversationFromNativeApp(long nativeThreadId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteNativeConversation: " + nativeThreadId);
        }
        updateXmsFlags();
    }

    @Override
    public void onReadXmsMessage(String messageId) {
        if (sLogger.isActivated()) {
            sLogger.info("onReadXmsMessage: ".concat(messageId));
        }
        updateXmsFlags();
    }

    @Override
    public void onReadXmsConversation(ContactId contactId) {
        boolean isLogActivated = sLogger.isActivated();
        if (isLogActivated) {
            sLogger.info("onReadXmsConversation: ".concat(contactId.toString()));
        }
        updateXmsFlags();
    }

    @Override
    public void onDeleteXmsMessage(String messageId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteXmsMessage: ".concat(messageId));
        }
        updateXmsFlags();
    }

    @Override
    public void onDeleteXmsConversation(ContactId contactId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteXmsConversation:".concat(contactId.toString()));
        }
        updateXmsFlags();
    }

    @Override
    public void onDeleteAllXmsMessage() {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteAllXmsMessage");
        }
        updateXmsFlags();
    }

    @Override
    public void onReadChatMessage(String messageId) {
        if (sLogger.isActivated()) {
            sLogger.info("onReadChatMessage " + messageId);
        }
        updateChatFlags();
    }

    @Override
    public void onDeleteChatMessage(String messageId) {
        if (sLogger.isActivated()) {
            sLogger.info("onDeleteChatMessage " + messageId);
        }
        updateChatFlags();
    }

    @SuppressWarnings("unchecked")
    private void updateXmsFlags() {

        EventFrameworkMode xmsMode = mSettings.getEventFrameworkForXms();
        if (EventFrameworkMode.DISABLED == xmsMode) {
            if (sLogger.isActivated()) {
                sLogger.debug("Event framework is not enabled for Xms messages");
            }
            return;
        }

        if (EventFrameworkMode.IMAP == xmsMode) {
            mImapEventFrameworkHandler.updateFlags(xmsMode, mSettings.getEventFrameworkForChat());
        } else if (EventFrameworkMode.SIP == xmsMode) {
            mSipEventFrameworkHandler.updateFlags(xmsMode, mSettings.getEventFrameworkForChat());
        }
    }

    @SuppressWarnings("unchecked")
    private void updateChatFlags() {

        EventFrameworkMode chatMode = mSettings.getEventFrameworkForChat();
        if (EventFrameworkMode.DISABLED == chatMode) {
            if (sLogger.isActivated()) {
                sLogger.debug("Event framework is not enabled for Chat messages");
            }
            return;
        }

        if (EventFrameworkMode.IMAP == chatMode) {
            mImapEventFrameworkHandler.updateFlags(mSettings.getEventFrameworkForXms(), chatMode);
        } else if (EventFrameworkMode.SIP == chatMode) {
            mSipEventFrameworkHandler.updateFlags(mSettings.getEventFrameworkForXms(), chatMode);
        }
    }
}
