/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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
 ******************************************************************************/

package com.gsma.rcs.cms.event;

import com.gsma.services.rcs.cms.XmsMessage.State;
import com.gsma.services.rcs.contact.ContactId;

public interface XmsMessageListener {

    void onReadXmsMessage(String messageId);

    void onDeleteXmsMessage(String messageId);

    void onReadXmsConversation(ContactId contact);

    void onDeleteXmsConversation(ContactId contact);

    void onXmsMessageStateChanged(ContactId contact, String messageId, String mimeType, State state);

    void onDeleteAllXmsMessage();

}
