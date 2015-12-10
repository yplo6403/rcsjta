// TODO add copyright
package com.gsma.rcs.cms;

import android.content.Context;

import com.gsma.rcs.cms.event.INativeXmsEventListener;
import com.gsma.rcs.cms.event.IRcsXmsEventListener;
import com.gsma.rcs.cms.event.XmsEventListener;
import com.gsma.rcs.cms.fordemo.ImapCommandController;
import com.gsma.rcs.cms.observer.XmsObserver;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.service.broadcaster.XmsMessageEventBroadcaster;
import com.gsma.services.rcs.contact.ContactId;

public class CmsManager implements IRcsXmsEventListener {

    private Context mContext;
    private XmsObserver mXmsObserver;
    private XmsEventListener mNewXmsEventListener;
    private LocalStorage mLocalStorage;
    private ImapLog mImapLog;
    private XmsLog mXmsLog;
    private RcsSettings mRcsSettings;
    private ImapCommandController mImapCommandController;

    /**
     * Instantiate the Cms CmsManager
     *
     * @param context
     * @return CmsManager
     */
    public CmsManager (Context context, ImapLog imapLog, XmsLog xmsLog, RcsSettings rcsSettings) {
        mContext = context;
        mRcsSettings = rcsSettings;
        mImapLog = imapLog;
        mXmsLog = xmsLog;
    }

    public void start() {

        // execute sync between providers with async task
        new ProviderSynchronizer(mContext, mRcsSettings, mXmsLog, mImapLog).execute();

        // instantiate Xms Observer on native SMS/MMS content provider
        mXmsObserver = new XmsObserver(mContext, mRcsSettings);

        // instantiate  XmsEventListener in charge of handling xms events from XmsObserver
        mNewXmsEventListener = new XmsEventListener(mContext, mImapLog, mXmsLog, mRcsSettings);
        mXmsObserver.registerListener(mNewXmsEventListener);

        // instantiate  LocalStorage in charge of handling events relatives to IMAP sync
        mLocalStorage = new LocalStorage(mImapLog);
        mLocalStorage.registerRemoteEventHandler(MessageType.SMS, mNewXmsEventListener);
        mLocalStorage.registerRemoteEventHandler(MessageType.MMS, mNewXmsEventListener);
        //mLocalStorage.registerRemoteEventHandler(MessageType.ONETOONE, tobedefined);
        //mLocalStorage.registerRemoteEventHandler(MessageType.GC, tobedefined);

        // instantiate ImapCommandController in charge of Pushing messages and updating flags with Imap command
        mImapCommandController = new ImapCommandController(mContext, mRcsSettings,mLocalStorage, mImapLog, mXmsLog);
        mXmsObserver.registerListener(mImapCommandController);

        // start content observer on native SMS/MMS content provider
        mXmsObserver.start();
    }

    public void stop() {
        if (mXmsObserver != null) {
            mXmsObserver.stop();
            mXmsObserver = null;
        }
        //TODO FGI : Fix me : when the core is stopped while a background task is still processing (IMAP sync)
//        if(mLocalStorage!=null){
//            mLocalStorage.removeListeners();
//            mLocalStorage = null;
//        }
//        mNewXmsEventListener = null;
//        mImapCommandController = null;
    }

    /**
     * @param listener
     */
    public void registerSmsObserverListener(INativeXmsEventListener listener) {
        if(mXmsObserver != null){
            mXmsObserver.registerListener(listener);
        }
    }

    /**
     * @param listener
     */
    public void unregisterSmsObserverListener(INativeXmsEventListener listener) {
        if(mXmsObserver != null){
            mXmsObserver.unregisterListener(listener);
        }
    }

    @Override
    public void onReadRcsMessage(String messageId) {
        if(mNewXmsEventListener != null){
            mNewXmsEventListener.onReadRcsMessage(messageId);
        }
        if(mImapCommandController != null) {
            mImapCommandController.onReadRcsMessage(messageId);
        }
    }

    @Override
    public void onDeleteRcsMessage(String messageId) {
        if(mNewXmsEventListener != null) {
            mNewXmsEventListener.onDeleteRcsMessage(messageId);
        }
        if(mImapCommandController != null) {
            mImapCommandController.onDeleteRcsMessage(messageId);
        }
    }

    @Override
    public void onReadRcsConversation(ContactId contact) {
        if(mNewXmsEventListener != null) {
            mNewXmsEventListener.onReadRcsConversation(contact);
        }
        if(mImapCommandController != null) {
            mImapCommandController.onReadRcsConversation(contact);
        }
    }

    @Override
    public void onDeleteRcsConversation(ContactId contact) {
        if(mNewXmsEventListener != null) {
            mNewXmsEventListener.onDeleteRcsConversation(contact);
        }
        if(mImapCommandController != null) {
            mImapCommandController.onDeleteRcsConversation(contact);
        }
    }

    @Override
    public void onDeleteAll() {
        if(mNewXmsEventListener != null) {
            mNewXmsEventListener.onDeleteAll();
        }
        if(mImapCommandController != null) {
            mImapCommandController.onDeleteAll();
        }
    }

    public void registerXmsMessageEventBroadcaster(XmsMessageEventBroadcaster xmsMessageEventBroadcaster){
        if(mNewXmsEventListener != null){
            mNewXmsEventListener.registerBroadcaster(xmsMessageEventBroadcaster);
        }
    }

    public void unregisterXmsMessageEventBroadcaster(XmsMessageEventBroadcaster xmsMessageEventBroadcaster){
        if(mNewXmsEventListener != null){
            mNewXmsEventListener.unregisterBroadcaster(xmsMessageEventBroadcaster);
        }
    }

    public Context getContext(){
        return mContext;
    }

    public XmsLog getXmsLog(){
        return mXmsLog;
    }

    public LocalStorage getLocalStorage(){
        return mLocalStorage;
    }

}
