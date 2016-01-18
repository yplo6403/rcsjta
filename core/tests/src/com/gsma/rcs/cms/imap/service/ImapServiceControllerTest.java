
package com.gsma.rcs.cms.imap.service;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class ImapServiceControllerTest extends AndroidTestCase {

    private RcsSettings mSettings;

    protected void setUp() throws Exception {
        super.setUp();
        mSettings = RcsSettings.createInstance(new LocalContentResolver(getContext()));
        mSettings.setCmsServerAddress("imap://myAddress");
        mSettings.setCmsUserLogin("myLogin");
        mSettings.setCmsUserPwd("myPwd");
    }

    public void test() throws NetworkException, PayloadException {
        ImapServiceController imapServiceController = new ImapServiceController(mSettings);
        imapServiceController.start();
        Assert.assertTrue(imapServiceController.isStarted());
        Assert.assertTrue(imapServiceController.isSyncAvailable());
        try {
            imapServiceController.createService();
            Assert.assertFalse(imapServiceController.isSyncAvailable());
        } catch (ImapServiceNotAvailableException e) {
            Assert.fail();
        }

        try {
            imapServiceController.createService();
            Assert.fail();
        } catch (ImapServiceNotAvailableException e) {
        }

        imapServiceController.closeService();
        Assert.assertTrue(imapServiceController.isSyncAvailable());
        imapServiceController.stop();
        Assert.assertFalse(imapServiceController.isStarted());
    }

    public void testRunnable() {

        final ImapServiceController imapServiceController = new ImapServiceController(mSettings);

        Runnable run1 = new Runnable() {
            @Override
            public void run() {
                try {
                    imapServiceController.createService();
                    Thread.sleep(100);
                    imapServiceController.closeService();
                } catch (InterruptedException | ImapServiceNotAvailableException | PayloadException
                        | NetworkException e) {
                    Assert.fail();
                }
            }
        };

        Runnable run2 = new Runnable() {
            @Override
            public void run() {
                try {
                    imapServiceController.createService();
                    Assert.fail();
                } catch (ImapServiceNotAvailableException e) {
                }
            }
        };

        Thread th1 = new Thread(run1);
        Thread th2 = new Thread(run2);
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
