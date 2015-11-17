/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.service.broadcaster;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.IXmsMessageListener;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageIntent;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by yplo6403 on 10/11/2015.
 */
public class XmsMessageEventBroadcaster implements IXmsMessageEventBroadcaster {

    private static final Logger sLogger = Logger.getLogger(XmsMessageEventBroadcaster.class
            .getName());
    private final RemoteCallbackList<IXmsMessageListener> mListeners = new RemoteCallbackList<>();

    public XmsMessageEventBroadcaster() {
    }

    public void addEventListener(IXmsMessageListener listener) {
        mListeners.register(listener);
    }

    public void removeEventListener(IXmsMessageListener listener) {
        mListeners.unregister(listener);
    }

    @Override
    public void broadcastMessageStateChanged(ContactId contact, String mimeType, String msgId,
            XmsMessage.State state, XmsMessage.ReasonCode reasonCode) {
        final int N = mListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mListeners.getBroadcastItem(i).onStateChanged(contact, mimeType, msgId,
                        state.toInt(), reasonCode.toInt());
            } catch (RemoteException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Can't notify listener.", e);
                }
            }
        }
        mListeners.finishBroadcast();
    }

    @Override
    public void broadcastNewMessage(String mimeType, String msgId) {
        Intent newXmsMessage = new Intent(XmsMessageIntent.ACTION_NEW_XMS_MESSAGE);
        newXmsMessage.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(newXmsMessage);
        newXmsMessage.putExtra(XmsMessageIntent.EXTRA_MIME_TYPE, mimeType);
        newXmsMessage.putExtra(XmsMessageIntent.EXTRA_MESSAGE_ID, msgId);
        AndroidFactory.getApplicationContext().sendBroadcast(newXmsMessage);
    }

    @Override
    public void broadcastMessageDeleted(ContactId contact, Set<String> messageIds) {
        final int N = mListeners.beginBroadcast();
        List<String> msgIds = new ArrayList<>(messageIds);
        for (int i = 0; i < N; i++) {
            try {
                mListeners.getBroadcastItem(i).onDeleted(contact, msgIds);
            } catch (RemoteException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Can't notify listener.", e);
                }
            }
        }
        mListeners.finishBroadcast();
    }
}
