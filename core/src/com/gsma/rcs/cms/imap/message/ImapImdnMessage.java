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

package com.gsma.rcs.cms.imap.message;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class ImapImdnMessage extends ImapCpimMessage {

    public ImapImdnMessage(com.sonymobile.rcs.imap.ImapMessage rawMessage) {
        super(rawMessage);
    }

    public ImdnDocument getImdnDocument() throws ParseFailureException, SAXException, ParserConfigurationException {
        return ChatUtils.parseCpimDeliveryReport(getCpimMessage().toPayload());
    }
}
