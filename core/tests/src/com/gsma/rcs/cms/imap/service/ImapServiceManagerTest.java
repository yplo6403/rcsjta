package com.gsma.rcs.cms.imap.service;

import com.gsma.rcs.cms.provider.settings.CmsSettings;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class ImapServiceManagerTest extends AndroidTestCase {
    
    private CmsSettings mSettings; 
    
    protected void setUp() throws Exception {
        super.setUp();                
        mSettings = CmsSettings.createInstance(getContext());
        mSettings.setMyNumber("myNumber");
        mSettings.setServerAddress("imap://myAddress");
        mSettings.setUserLogin("myLogin");
        mSettings.setUserPwd("myPwd");
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
