package com.gsma.rcs.cms.imap.service;

import android.test.AndroidTestCase;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

import junit.framework.Assert;

public class ImapServiceManagerTest extends AndroidTestCase {
    
    private RcsSettings mSettings;
    
    protected void setUp() throws Exception {
        super.setUp();                
        mSettings = RcsSettings.createInstance(new LocalContentResolver(getContext()));
        mSettings.setCmsServerAddress("imap://myAddress");
        mSettings.setCmsUserLogin("myLogin");
        mSettings.setCmsUserPwd("myPwd");
    }
    
    public void test(){
    
        
        BasicImapService imapService = null;
        try {
            imapService= ImapServiceManager.getService(mSettings);
        } catch (ImapServiceNotAvailableException e) {
            Assert.fail();
        }
        
        try {
            imapService = ImapServiceManager.getService(mSettings);
            Assert.fail();
        } catch (ImapServiceNotAvailableException e) {            
        }

        ImapServiceManager.releaseService(imapService);
        
    }
    
    public void testRunnable(){
        
        Runnable run1 = new Runnable(){
            @Override
            public void run() {
                try {
                    BasicImapService imapService= ImapServiceManager.getService(mSettings);
                    Thread.sleep(100);
                    ImapServiceManager.releaseService(imapService);
                } catch (ImapServiceNotAvailableException e) {
                    Assert.fail();
                }catch (InterruptedException e) {
                    Assert.fail();
                }
            }            
        };
        
        Runnable run2 = new Runnable(){
            @Override
            public void run() {
                try {
                    ImapServiceManager.getService(mSettings);
                    Assert.fail();
                } catch (ImapServiceNotAvailableException e) {
                }
            }            
        };
        
        Thread th1 =  new Thread(run1);
        Thread th2 =  new Thread(run2);
        th1.start();
        th2.start();
        try {
            th1.join();
            th2.join();
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

}
