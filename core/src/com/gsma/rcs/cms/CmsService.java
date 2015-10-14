
package com.gsma.rcs.cms;

import com.gsma.rcs.cms.event.INativeSmsEventListener;
import com.gsma.rcs.cms.event.IRcsSmsEventListener;
import com.gsma.rcs.cms.event.SmsEventHandler;
import com.gsma.rcs.cms.fordemo.ImapCommandController;
import com.gsma.rcs.cms.observer.SmsObserver;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;

public class CmsService implements IRcsSmsEventListener {

    private static Logger sLogger = Logger.getLogger(CmsService.class.getName());

    private static CmsService sInstance;

    private Context mContext;
    private SmsObserver mSmsObserver;
    private SmsEventHandler mSmsEventHandler;

    // TO BE REMOVED : demo purpose only
    private ImapCommandController mImapCommandController;

    /**
     * Instantiate the Cms Service
     * 
     * @param context
     * @return CmsService
     */
    public static CmsService createInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (CmsService.class) {
            if (sInstance == null) {
                sInstance = new CmsService(context);
            }
        }
        return sInstance;
    }

    private CmsService(Context context) {
        mContext = context;
        CmsSettings cmsSettings = CmsSettings.createInstance(mContext);

        XmsLog xmsLog = XmsLog.getInstance(mContext);
        ImapLog imapLog = ImapLog.getInstance(mContext);

        // will be in charge of handling sms event
        mSmsEventHandler = new SmsEventHandler(imapLog, xmsLog);

        // start content observer on native SMS/MMS content provider
        mSmsObserver = SmsObserver.createInstance(mContext);
        mSmsObserver.registerListener(mSmsEventHandler);
        mSmsObserver.start();

        LocalStorage localStorage = LocalStorage.createInstance(imapLog);
        localStorage.registerRemoteEventHandler(MessageType.SMS, mSmsEventHandler);
        localStorage.addLocalEventHandler(MessageType.SMS, mSmsEventHandler);
        // TODO FGI : register listener for each type of messages

        // ---*** begin : should be removed
        mImapCommandController = ImapCommandController.createInstance(mContext, cmsSettings,
                imapLog, xmsLog);
        mSmsObserver.registerListener(mImapCommandController);
        // ---*** end :
    }

    /**
     * Returns the singleton instance
     * 
     * @return CmsService instance
     */
    public static CmsService getInstance() {
        return sInstance;
    }

    /**
     * @param listener
     */
    public void registerSmsObserverListener(INativeSmsEventListener listener) {
        if(sLogger.isActivated()){
            sLogger.debug("registerSmsObserverListener : ".concat(listener.toString()));
        }
        mSmsObserver.registerListener(listener);
    }

    /**
     * @param listener
     */
    public void unregisterSmsObserverListener(INativeSmsEventListener listener) {
        if(sLogger.isActivated()){
            sLogger.debug("unregisterSmsObserverListener : ".concat(listener.toString()));
        }
        mSmsObserver.unregisterListener(listener);
    }

    @Override
    public void onReadRcsConversation(String contact) {
        mSmsEventHandler.onReadRcsConversation(contact);
        mImapCommandController.onReadRcsConversation(contact);
    }

    @Override
    public void onDeleteRcsSms(String contact, String baseId) {
        mSmsEventHandler.onDeleteRcsSms(contact, baseId);
        mImapCommandController.onDeleteRcsSms(contact, baseId);
    }

    @Override
    public void onDeleteRcsConversation(String contact) {
        mSmsEventHandler.onDeleteRcsConversation(contact);
        mImapCommandController.onDeleteRcsConversation(contact);
    }

    @Override
    public void onReadRcsMessage(String contact, String baseId) {
        mSmsEventHandler.onReadRcsMessage(contact, baseId);
        mImapCommandController.onReadRcsMessage(contact, baseId);
    }
}
