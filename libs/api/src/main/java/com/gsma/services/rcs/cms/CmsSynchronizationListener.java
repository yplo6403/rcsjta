/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs.cms;

import com.gsma.services.rcs.contact.ContactId;

/**
 * CMS synchronization event listener
 *
 * @author Philippe LEMORDANT
 */
public abstract class CmsSynchronizationListener {

    /**
     * Callback called when global synchronization operation is completed.
     *
     */
    public abstract void onAllSynchronized();

    /**
     * Callback called when a one to one conversation synchronization operation is completed.
     *
     * @param contact Contact ID
     */
    public abstract void onOneToOneConversationSynchronized(ContactId contact);

    /**
     * Callback called when a group conversation synchronization operation is completed.
     *
     * @param chatId chat ID
     */
    public abstract void onGroupConversationSynchronized(String chatId);
}