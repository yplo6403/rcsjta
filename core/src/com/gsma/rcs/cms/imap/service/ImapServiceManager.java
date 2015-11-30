
package com.gsma.rcs.cms.imap.service;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.ImapService;
import com.sonymobile.rcs.imap.IoService;
import com.sonymobile.rcs.imap.SocketIoService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 *
 */
public class ImapServiceManager {

    private static final Logger sLogger = Logger.getLogger(ImapServiceManager.class.getSimpleName());    
    
    private static final List<ImapServiceListener> mListeners = new ArrayList<ImapServiceListener>();
    private static Semaphore sSemaphore = new Semaphore(1);
    private static boolean mAvailable = true;
    
    /**
     * @param settings 
     * @return BasicImapService
     * @throws ImapServiceNotAvailableException
     */
    public static BasicImapService getService(RcsSettings settings) throws ImapServiceNotAvailableException {

        if (!sSemaphore.tryAcquire()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Can not acquire a token for synchronization yet ... ");
                sLogger.debug("A previous one is is already in progress ... ");
            }
            throw new ImapServiceNotAvailableException("A imap sync is already in progress. Can not start a new one yet.");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Acquire a token for synchronization ... ");
        }
        mAvailable = false;
        IoService io = new SocketIoService(settings.getCmsServerAddress());
        // ImapService service = new DefaultImapService(io);
        BasicImapService service = new BasicImapService(io);
        service.setAuthenticationDetails(settings.getCmsUserLogin(),
                settings.getCmsUserPwd(), null, null, false);
        return service;        
    }
      
    /**
     * @param imapService
     */
    public static void releaseService(ImapService imapService) {        
        if(sLogger.isActivated()){
            sLogger.debug("Release the token for synchronization ... ");
        }        
        try {
            if(imapService.isAvailable()){
                imapService.logout();
                imapService.close();                                        
            }
        } catch (Exception e) {
            //TODO FGI : handle properly exception
            e.printStackTrace();
        }  
        finally{
            sSemaphore.release();
            mAvailable = true;            
            for (ImapServiceListener listener : mListeners) {
                listener.onImapServiceAvailable();
            }
            mListeners.clear();            
        }
    }
    
    /**
     * @return
     */
    public static boolean isAvailable(){
        return mAvailable;
    }
    
    /**
     * @param listener
     */
    public static void registerListener(ImapServiceListener listener){
        synchronized(mListeners){
            mListeners.add(listener);    
        }        
    }

    /**
     * @param listener
     */
    public static void unregisterListener(ImapServiceListener listener){
        synchronized(mListeners){
            mListeners.remove(listener);
        }        
    }
    
    /**
    *
    */
   public interface ImapServiceListener {
       void onImapServiceAvailable();
   }
}
