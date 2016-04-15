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
 *
 ******************************************************************************/

package com.gsma.rcs.core.cms.event.framework;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.MessageType;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.contact.ContactId;

import java.util.List;

public class SipEventFrameworkDocument {

    private static final Logger sLogger = Logger.getLogger(SipEventFrameworkDocument.class
            .getSimpleName());

    private final static String XML_OBJECT_WITH_UID = "<object uid=\"%1$s\" folder-path=\"%2$s/\">"
            + "<message-id>%3$s</message-id>" + "</object>";

    private final static String XML_CHAT_MESSAGE = "<object>"
            + "<conversation-id>%1$s</conversation-id>" + "<contribution-id>%2$s</contribution-id>"
            + "<other-party>%3$s</other-party>" + "<message-id>%4$s</message-id>" + "</object>";

    private final static String XML_GCHAT_MESSAGE = "<object>"
            + "<conversation-id>%1$s</conversation-id>" + "<contribution-id>%2$s</contribution-id>"
            + "<message-id>%3$s</message-id>" + "</object>";

    // List is required to keep insertion order from content provider
    private final List<CmsObject> mSeenObject;
    // List is required to keep insertion order from content provider
    private final List<CmsObject> mDeletedObject;

    public SipEventFrameworkDocument(List<CmsObject> seenObjects,
                                     List<CmsObject> deletedObjects) {
        mSeenObject = seenObjects;
        mDeletedObject = deletedObjects;
    }

    public String toXml() {
        StringBuilder sb = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<cpm-evfw xmlns=\"urn:oma:xml:cpm:evfw\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                        + "<event-cpm-imap type=\"flags\" index=\"0\">");
        if (!mSeenObject.isEmpty()) {
            sb.append("<operation name=\"add\" flag=\"\\Seen\">");
            for (CmsObject object : mSeenObject) {
                sb.append(toXml(object));
            }
            sb.append("</operation>");
        }
        if (!mDeletedObject.isEmpty()) {
            sb.append("<operation name=\"add\" flag=\"\\Deleted\">");
            for (CmsObject object : mDeletedObject) {
                sb.append(toXml(object));
            }
            sb.append("</operation>");
        }
        sb.append("</event-cpm-imap></cpm-evfw>");
        return sb.toString();
    }

    private String toXml(CmsObject object) {
        Integer uid = object.getUid();
        if (uid != null) {
            return String.format(XML_OBJECT_WITH_UID, object.getUid(), object.getFolder(),
                    object.getMessageId());
        }
        MessageType messageType = object.getMessageType();
        if (MessageType.SMS == messageType || MessageType.MMS == messageType) {
            if (sLogger.isActivated()) {
                sLogger.debug("Sip event reporting framework not implemented for XMS messages having no uid");
            }
            return "";
        }
        if (MessageType.CHAT_MESSAGE == messageType || MessageType.FILE_TRANSFER == messageType) {
            String folder = object.getFolder();
            ContactId contact = CmsUtils.cmsFolderToContact(folder);
            if (contact != null) { // 1-1
                // For 1To1 conversation in Simple IM mode, contribution-Id and conversation-Id are
                // not available.
                // They are filled with message-Id value
                return String.format(XML_CHAT_MESSAGE, object.getMessageId(),
                        object.getMessageId(), Constants.TEL_PREFIX + contact.toString(),
                        object.getMessageId());
            } else { // GC
                String chatId = CmsUtils.cmsFolderToChatId(folder);
                return String.format(XML_GCHAT_MESSAGE, chatId, chatId, object.getMessageId());
            }
        }
        return "";
    }
}
