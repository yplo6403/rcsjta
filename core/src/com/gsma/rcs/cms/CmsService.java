
package com.gsma.rcs.cms;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.event.RcsXmsEventListener;
import com.gsma.rcs.cms.event.XmsEventHandler;
import com.gsma.rcs.cms.fordemo.ImapCommandController;
import com.gsma.rcs.cms.observer.XmsObserver;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;

public class CmsService implements RcsXmsEventListener {

    private static Logger sLogger = Logger.getLogger(CmsService.class.getName());

    private static CmsService sInstance;

    private Context mContext;
    private XmsObserver mXmsObserver;
    private XmsEventHandler mXmsEventHandler;

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

        FileFactory.createDirectory(MmsUtils.MMS_DIRECTORY_PATH);

        XmsLog xmsLog = XmsLog.getInstance(context);
        PartLog partLog = PartLog.getInstance(context);
        ImapLog imapLog = ImapLog.getInstance(context);

        // will be in charge of handling sms event
        mXmsEventHandler = new XmsEventHandler(imapLog, xmsLog, partLog);

        // start content observer on native SMS/MMS content provider
        mXmsObserver = XmsObserver.createInstance(mContext);
        mXmsObserver.registerListener(mXmsEventHandler);
        mXmsObserver.start();

        LocalStorage localStorage = LocalStorage.createInstance(imapLog);
        localStorage.registerRemoteEventHandler(MessageType.SMS, mXmsEventHandler);
        localStorage.registerRemoteEventHandler(MessageType.MMS, mXmsEventHandler);
        localStorage.addLocalEventHandler(MessageType.SMS, mXmsEventHandler);
        localStorage.addLocalEventHandler(MessageType.MMS, mXmsEventHandler);
        // TODO FGI : register listener for each type of messages

        // ---*** begin : should be removed
        mImapCommandController = ImapCommandController.createInstance(mContext, cmsSettings,
                imapLog, xmsLog, partLog);
        mXmsObserver.registerListener(mImapCommandController);
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
    public void registerSmsObserverListener(INativeXmsEventListener listener) {
        if(sLogger.isActivated()){
            sLogger.debug("registerSmsObserverListener : ".concat(listener.toString()));
        }
        mXmsObserver.registerListener(listener);
    }

    /**
     * @param listener
     */
    public void unregisterSmsObserverListener(INativeXmsEventListener listener) {
        if(sLogger.isActivated()){
            sLogger.debug("unregisterSmsObserverListener : ".concat(listener.toString()));
        }
        mXmsObserver.unregisterListener(listener);
    }

    @Override
    public void onReadRcsConversation(String contact) {
        mXmsEventHandler.onReadRcsConversation(contact);
        mImapCommandController.onReadRcsConversation(contact);
    }

    @Override
    public void onDeleteRcsSms(String contact, String baseId) {
        mXmsEventHandler.onDeleteRcsSms(contact, baseId);
        mImapCommandController.onDeleteRcsSms(contact, baseId);
    }

    @Override
    public void onDeleteRcsMms(String contact, String baseId, String mms_id) {
        mXmsEventHandler.onDeleteRcsMms(contact, baseId, mms_id);
        mImapCommandController.onDeleteRcsMms(contact, baseId,  mms_id);
    }

    @Override
    public void onDeleteRcsConversation(String contact) {
        mXmsEventHandler.onDeleteRcsConversation(contact);
        mImapCommandController.onDeleteRcsConversation(contact);
    }

    @Override
    public void onReadRcsMessage(String contact, String baseId) {
        mXmsEventHandler.onReadRcsMessage(contact, baseId);
        mImapCommandController.onReadRcsMessage(contact, baseId);
    }

    public Context getContext(){
        return mContext;
    }
}
