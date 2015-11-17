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

import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.ICmsSynchronizationListener;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

/**
 * Created by yplo6403 on 10/11/2015.
 */
public class CmsEventBroadcaster implements ICmsEventBroadcaster {

    private final RemoteCallbackList<ICmsSynchronizationListener> mListeners = new RemoteCallbackList<>();

    private static final Logger sLogger = Logger.getLogger(CmsEventBroadcaster.class.getName());

    public CmsEventBroadcaster() {
    }

    public void addEventListener(ICmsSynchronizationListener listener) {
        mListeners.register(listener);
    }

    public void removeEventListener(ICmsSynchronizationListener listener) {
        mListeners.unregister(listener);
    }

    @Override
    public void broadcastOneToOneConversationSynchronized(ContactId contact) {
        final int N = mListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mListeners.getBroadcastItem(i).onOneToOneConversationSynchronized(contact);
            } catch (RemoteException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Can't notify listener.", e);
                }
            }
        }
        mListeners.finishBroadcast();
    }

    @Override
    public void broadcastAllSynchronized() {
        final int N = mListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mListeners.getBroadcastItem(i).onAllSynchronized();
            } catch (RemoteException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Can't notify listener.", e);
                }
            }
        }
        mListeners.finishBroadcast();
    }

    @Override
    public void broadcastGroupConversationSynchronized(String chatId) {
        final int N = mListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mListeners.getBroadcastItem(i).onGroupConversationSynchronized(chatId);
            } catch (RemoteException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Can't notify listener.", e);
                }
            }
        }
        mListeners.finishBroadcast();
    }
}
