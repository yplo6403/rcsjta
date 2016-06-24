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
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Formatter;
import java.util.Locale;

/**
 * @author Philippe LEMORDANT.
 */
public class ImapOneToOneChatMessageImpl implements ImapCmsUtilTest.IImapRcsMessage {

    private final String mChatId;
    private final RcsService.Direction mDirection;
    private final long mTimestamp;
    private final ContactId mFromContact;
    private final ContactId mToContact;
    private final String mMessageId;
    private final ContactId mRemote;
    private final ContactId mLocal;
    private final String mFolder;
    private boolean mSeen;
    private final boolean mDeleted;
    private final String mContent;
    private Integer mUid;

    // @formatter:off

    private static final String ONE2ONE_CHAT_MESSAGE = "Date: %1$s\n" +
            "From: tel:%2$s\n" +
            "To: tel:%3$s\n" +
            "Message-ID: <351777483.2208.1458899140811@RCS5frontox1>\n" +
            "MIME-Version: 1.0\n" +
            "Content-Type: message/cpim\n" +
            "Content-Transfer-Encoding: 8bit\n" +
            "Conversation-ID: %4$s\n" +
            "Contribution-ID: %4$s\n" +
            "IMDN-Message-ID: %5$s\n" +
            "Message-Direction: %6$s\n" +
            "\n" +
            "From: <sip:anonymous@anonymous.invalid>\n" +
            "To: <sip:anonymous@anonymous.invalid>\n" +
            "NS: imdn <urn:ietf:params:imdn>\n" +
            "imdn.Message-ID: %5$s\n" +
            "DateTime: %7$s\n" +
            "imdn.Disposition-Notification: positive-delivery\n" +
            "\n" +
            "Content-type: text/plain;charset=utf-8\n" +
            "Content-length: %8$d\n" +
            "\n" +
            "%9$s";

    // @formatter:on

    public ImapOneToOneChatMessageImpl(ContactId fromContact, ContactId toContact,
            RcsService.Direction dir, long timestamp, String content, boolean seen, boolean deleted) {
        mDirection = dir;
        mTimestamp = timestamp;
        mFromContact = fromContact;
        mToContact = toContact;
        mMessageId = IdGenerator.generateMessageID();
        mContent = content;
        mSeen = seen;
        mDeleted = deleted;
        if (RcsService.Direction.INCOMING == mDirection) {
            mRemote = mFromContact;
            mLocal = mToContact;
        } else {
            mRemote = mToContact;
            mLocal = mFromContact;
        }
        mChatId = mRemote.toString();
        mFolder = CmsUtils.contactToCmsFolder(mRemote);
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
        String imapDate = DateUtils.getDateAsString(System.currentTimeMillis(),
                DateUtils.CMS_IMAP_DATE_FORMAT);
        String cpimDate = DateUtils.getDateAsString(mTimestamp, DateUtils.CMS_CPIM_DATE_FORMAT);
        formatter
                .format(ONE2ONE_CHAT_MESSAGE, imapDate, mFromContact.toString(),
                        mToContact.toString(), mChatId, mMessageId,
                        ImapCmsUtilTest.convertDirToImap(mDirection), cpimDate, mContent.length(),
                        mContent);
        return formatter.toString();
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

    public String getContent() {
        return mContent;
    }

    public String getMimeType() {
        return "text/plain";
    }

    public ContactId getLocalContact() {
        return mLocal;
    }
}
