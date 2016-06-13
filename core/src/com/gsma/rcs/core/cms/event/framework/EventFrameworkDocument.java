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
import com.gsma.services.rcs.contact.ContactId;

import java.util.List;

public class EventFrameworkDocument {

    private final static String XML_OBJECT_WITH_UID = "<object uid=\"%1$s\" folder-path=\"%2$s/\">"
            + "<message-id>%3$s</message-id>" + "</object>";

    // List is required to keep insertion order from content provider
    private final List<CmsObject> mSeenObject;
    // List is required to keep insertion order from content provider
    private final List<CmsObject> mDeletedObject;
    private final String mContributionId;

    /**
     * A class to format SIP event framework document
     * 
     * @param seenObjects the list of seen objects
     * @param deletedObjects the list of deleted objects
     * @param contributionId the contribution ID
     */
    public EventFrameworkDocument(List<CmsObject> seenObjects, List<CmsObject> deletedObjects,
            String contributionId) {
        mSeenObject = seenObjects;
        mDeletedObject = deletedObjects;
        mContributionId = contributionId;
    }

    /**
     * Format document to XML
     * 
     * @return the XML representation of the event framework document
     */
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

    private String objectToXml(ContactId contact, String contributionId, String conversationId,
            String messageId) {
        StringBuilder xml = new StringBuilder("<object>");
        if (conversationId != null) {
            xml.append("<conversation-id>").append(conversationId).append("</conversation-id>");
        }
        if (contributionId != null) {
            xml.append("<contribution-id>").append(contributionId).append("</contribution-id>");
        }
        if (contact != null) {
            xml.append("<other-party>").append(Constants.TEL_PREFIX).append(contact.toString())
                    .append("</other-party>");
        }
        xml.append("<message-id>").append(messageId).append("</message-id>");
        xml.append("</object>");
        return xml.toString();
    }

    private String toXml(CmsObject object) {
        Integer uid = object.getUid();
        if (uid != null) {
            return String.format(XML_OBJECT_WITH_UID, object.getUid(), object.getFolder(),
                    object.getMessageId());
        }
        MessageType messageType = object.getMessageType();
        switch (messageType) {
            case CHAT_MESSAGE:
            case FILE_TRANSFER:
            case MMS:
            case SMS:
                String folder = object.getFolder();
                ContactId contact = CmsUtils.cmsFolderToContact(folder);
                /*
                 * For GC, contact is null.
                 */
                return objectToXml(contact, mContributionId, mContributionId, object.getMessageId());

            default:
                return "";
        }
    }

    public List<CmsObject> getDeletedObject() {
        return mDeletedObject;
    }

    public List<CmsObject> getSeenObject() {
        return mSeenObject;
    }
}
