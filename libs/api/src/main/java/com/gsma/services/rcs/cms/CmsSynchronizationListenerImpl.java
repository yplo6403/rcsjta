/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.services.rcs.cms;

import com.gsma.services.rcs.contact.ContactId;

/**
 * CMS synchronization event listener implementation
 *
 * @hide
 */
public class CmsSynchronizationListenerImpl extends ICmsSynchronizationListener.Stub {

    private final CmsSynchronizationListener mListener;

    /* package private */CmsSynchronizationListenerImpl(CmsSynchronizationListener listener) {
        mListener = listener;
    }

    @Override
    public void onAllSynchronized() {
        mListener.onAllSynchronized();
    }

    @Override
    public void onOneToOneConversationSynchronized(ContactId contact) {
        mListener.onOneToOneConversationSynchronized(contact);
    }

    @Override
    public void onGroupConversationSynchronized(String chatId) {
        mListener.onGroupConversationSynchronized(chatId);
    }
}
