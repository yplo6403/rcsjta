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

package com.gsma.rcs.core.cms.protocol.message;

import com.gsma.rcs.core.cms.integration.ImapCmsUtilTest;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Formatter;
import java.util.Locale;

/**
 * @author Philippe LEMORDANT.
 */
public class ImapImdnMessageImpl implements ImapCmsUtilTest.IImapRcsMessage {

    private final String mChatId;
    private final RcsService.Direction mDirection;
    private final long mTimestamp;
    private final ContactId mFromContact;
    private final ContactId mToContact;
    private final String mMessageId;
    private final ContactId mRemote;
    private final String mXmlMessageId;
    private final String mFolder;
    private boolean mSeen;
    private final boolean mDeleted;
    private final ImdnDocument.DeliveryStatus mStatus;
    private final boolean mSingleChat;
    private Integer mUid;

    // @formatter:off

    private static final String IMDN_CHAT_CONTENT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">\n" +
            "<message-id>%1$s</message-id>\n" +
            "<datetime>%2$s</datetime>\n" +
            "<%3$s><status><%4$s/></status></%3$s>\n" +
            "</imdn>";

    private static final String IMDN_CHAT_HEADER = "Date: %1$s\n" +
            "From: tel:%2$s\n" +
            "To: tel:%3$s\n" +
            "Message-ID: <1457949707.11056.1465485444561@RCS5frontox1>\n" +
            "MIME-Version: 1.0\n" +
            "Content-Type: message/cpim\n" +
            "Content-Transfer-Encoding: 8bit\n" +
            "IMDN-Message-ID: %4$s\n" +
            "Message-Direction: %5$s\n" +
            "\n" +
            "From: %6$s\n" +
            "To: <sip:anonymous@anonymous.invalid>\n" +
            "NS: imdn <urn:ietf:params:imdn>\n" +
            "DateTime: %7$s\n" +
            "imdn.Message-ID: %4$s\n" +
            "Content-Disposition: notification\n" +
            "\n" +
            "Content-type: message/imdn+xml\n" +
            "Content-length: %8$d\n" +
            "\n" ;

    // @formatter:on

    public ImapImdnMessageImpl(String messageId, String chatId, ContactId fromContact,
            ContactId toContact, RcsService.Direction dir, long timestamp,
            ImdnDocument.DeliveryStatus status, boolean seen, boolean deleted) {
        mDirection = dir;
        mTimestamp = timestamp;
        mFromContact = fromContact;
        mToContact = toContact;
        mXmlMessageId = messageId;
        mMessageId = IdGenerator.generateMessageID();
        mStatus = status;
        mSeen = seen;
        mDeleted = deleted;
        if (RcsService.Direction.INCOMING == mDirection) {
            mRemote = mToContact;
        } else {
            mRemote = mFromContact;
        }
        if (chatId == null) {
            mChatId = mRemote.toString();
            mFolder = CmsUtils.contactToCmsFolder(mRemote);
            mSingleChat = true;
        } else {
            mChatId = chatId;
            mFolder = CmsUtils.groupChatToCmsFolder(chatId, chatId);
            mSingleChat = false;
        }

    }

    @Override
    public String getFolder() {
        return mFolder;
    }

    @Override
    public Integer getUid() {
        return mUid;
    }

    @Override
    public String toPayload() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        String xmlDate = com.gsma.rcs.utils.DateUtils.encodeDate(mTimestamp);
        String notif = (ImdnDocument.DeliveryStatus.DELIVERED == mStatus) ? ImdnDocument.DELIVERY_NOTIFICATION
                : ImdnDocument.DISPLAY_NOTIFICATION;
        formatter.format(IMDN_CHAT_CONTENT, mXmlMessageId, xmlDate, notif, mStatus.toString());
        String xmlContent = formatter.toString();
        long currentTimestamp = System.currentTimeMillis();
        String imapDate = DateUtils.getDateAsString(currentTimestamp,
                DateUtils.CMS_IMAP_DATE_FORMAT);
        String cpimDate = DateUtils.getDateAsString(currentTimestamp,
                DateUtils.CMS_CPIM_DATE_FORMAT);
        sb = new StringBuilder();
        formatter = new Formatter(sb, Locale.US);
        formatter.format(
                IMDN_CHAT_HEADER,
                imapDate,
                mFromContact.toString(),
                mToContact.toString(),
                mMessageId,
                ImapCmsUtilTest.convertDirToImap(mDirection),
                (mSingleChat) ? "<sip:anonymous@anonymous.invalid>" : "tel:"
                        + mFromContact.toString(), cpimDate, xmlContent.length() + 2);
        return formatter.toString() + xmlContent;
    }

    @Override
    public boolean isSeen() {
        return mSeen;
    }

    @Override
    public boolean isDeleted() {
        return mDeleted;
    }

    @Override
    public String getHeader(String headerName) {
        return null;
    }

    @Override
    public BodyPart getBodyPart() {
        return null;
    }

    @Override
    public String getChatId() {
        return mChatId;
    }

    @Override
    public String getMessageId() {
        return mMessageId;
    }

    @Override
    public void setUid(Integer uid) {
        mUid = uid;
    }

    @Override
    public ContactId getRemote() {
        return mRemote;
    }

    @Override
    public void markAsSeen() {
        mSeen = true;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    public ImdnDocument.DeliveryStatus getStatus() {
        return mStatus;
    }

}
