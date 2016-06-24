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
import java.util.List;
import java.util.Locale;

/**
 * @author Philippe LEMORDANT.
 */
public class ImapGroupStateMessageImpl implements ImapCmsUtilTest.IImapRcsMessage {

    private final String mChatId;
    private final RcsService.Direction mDirection;
    private final long mTimestamp;
    private final ContactId mToContact;
    private final String mMessageId;
    private final String mFolder;
    private final String mSubject;
    private final List<ContactId> mParticipants;
    private boolean mSeen;
    private final boolean mDeleted;
    private Integer mUid;

    // @formatter:off

    private static final String GROUP_STATE_XML_PARTICIPANT =
            "  <participant name=\"\" comm-addr=\"tel:%1$s\"/>\n";

    private static final String GROUP_STATE_XML_CONTENT = "<?xml version=\"1.0\"?>\n" +
            "<groupstate timestamp=\"%1$s\" " +
            "lastfocussessionid=\"sip:pfcf-imas-orange@rcs5mdsip1.sip.imsnsn.fr:5060;" +
            "transport=udp;oaid=+33788878935;ocid=c6cca468b4ade657694b9e75baeb9e93\" " +
            "group-type=\"Open\">\n" +
            "%2$s\n" +
            "</groupstate>";

    private static final String GROUP_STATE_MESSAGE = "Date: %1$s\n" +
            "From: sip:anonymous@anonymous.invalid\n" +
            "To: tel:%2$s\n" +
            "Message-ID: <454013394.11298.1465906190374@RCS5frontox1>\n" +
            "Subject: %3$s\n" +
            "MIME-Version: 1.0\n" +
            "Content-Type: Application/group-state-object+xml\n" +
            "Content-Transfer-Encoding: 8bit\n" +
            "Conversation-ID: %4$s\n" +
            "Contribution-ID: %4$s\n" +
            "IMDN-Message-ID: %5$s\n" +
            "Message-Direction: %6$s\n" +
            "\n";

    // @formatter:on

    public ImapGroupStateMessageImpl(String chatId, ContactId toContact, String subject,
            List<ContactId> participants, RcsService.Direction dir, long timestamp, boolean seen,
            boolean deleted) {
        mDirection = dir;
        mTimestamp = timestamp;
        mToContact = toContact;
        mMessageId = IdGenerator.generateMessageID();
        mSeen = seen;
        mDeleted = deleted;
        mChatId = chatId;
        mFolder = CmsUtils.groupChatToCmsFolder(chatId, chatId);
        mSubject = subject;
        mParticipants = participants;
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
        StringBuilder xmlParticipantsList = new StringBuilder();
        for (ContactId participant : mParticipants) {
            formatter.format(GROUP_STATE_XML_PARTICIPANT, participant.toString());
            xmlParticipantsList.append(formatter.toString());
        }
        sb = new StringBuilder();
        formatter = new Formatter(sb, Locale.US);
        String xmlDate = com.gsma.rcs.utils.DateUtils.encodeDate(mTimestamp);
        formatter.format(GROUP_STATE_XML_CONTENT, xmlDate, xmlParticipantsList.toString());
        String xmlContent = formatter.toString();

        long currentTimestamp = System.currentTimeMillis();
        String imapDate = DateUtils.getDateAsString(currentTimestamp,
                DateUtils.CMS_IMAP_DATE_FORMAT);

        sb = new StringBuilder();
        formatter = new Formatter(sb, Locale.US);
        formatter.format(GROUP_STATE_MESSAGE, imapDate, mToContact.toString(), mSubject, mChatId,
                mMessageId, ImapCmsUtilTest.convertDirToImap(mDirection));
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
        return null;
    }

    @Override
    public void markAsSeen() {
        mSeen = true;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    public String getSubject() {
        return mSubject;
    }

    public List<ContactId> getParticipants() {
        return mParticipants;
    }
}
