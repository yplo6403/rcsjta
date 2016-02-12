/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.protocol.service;

import com.gsma.rcs.core.cms.integration.RcsSettingsMock;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.test.AndroidTestCase;

import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.imaplib.imap.IoService;
import com.gsma.rcs.imaplib.imap.SocketIoService;

import junit.framework.Assert;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;

public class BasicImapServiceTest extends AndroidTestCase {

    private static final Logger sLogger = Logger
            .getLogger(BasicImapServiceTest.class.getSimpleName());

    private RcsSettings mSettings;
    private boolean mIsBlocked = true;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContactUtil.getInstance(getContext());
        mSettings = RcsSettingsMock.getMockSettings(context);
    }

    public void testWithoutSoTimeout() throws IOException, ImapException, InterruptedException {
        final IoService io = new SocketIoService(URI.create(mSettings.getMessageStoreUri().toString()));
        final BasicImapService service = new BasicImapService(io);
        service.setAuthenticationDetails(mSettings.getMessageStoreUser(),
                mSettings.getMessageStorePwd(), null, null, false);
        service.init();
        mIsBlocked = true;

        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    io.readLine();
                } catch (Exception ignore) {
                }
                mIsBlocked = false;
            }
        });

        myThread.start();
        // wait 5 seconds
        sLogger.info("wait for 5 seconds ...");
        myThread.join(5000);
        sLogger.info("after join, check if thread is always waiting on IO");
        Assert.assertTrue(mIsBlocked);
        myThread.interrupt();
        service.close();
    }

    public void testWithSoTimeout() throws IOException, ImapException, InterruptedException {
        final IoService io = new SocketIoService(URI.create(mSettings.getMessageStoreUri().toString()), 3000);
        final BasicImapService service = new BasicImapService(io);
        service.setAuthenticationDetails(mSettings.getMessageStoreUser(),
                mSettings.getMessageStorePwd(), null, null, false);
        service.init();
        mIsBlocked = true;

        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    io.readLine();
                } catch (SocketTimeoutException e) {
                    sLogger.info("--> SocketTimeoutException");
                    mIsBlocked = false;
                } catch (Exception ignore) {
                }
            }
        });

        myThread.start();
        // wait 5 seconds
        sLogger.info("wait for 5 seconds ...");
        myThread.join(5000);
        sLogger.info("after join, check if thread is always waiting on IO");
        Assert.assertFalse(mIsBlocked);
        service.close();
    }

}
