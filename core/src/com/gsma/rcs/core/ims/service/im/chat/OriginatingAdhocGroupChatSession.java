/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.im.chat;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.service.CmsManager;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.text.TextUtils;

import java.text.ParseException;
import java.util.Map;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.RequireHeader;
import javax2.sip.header.SubjectHeader;

/**
 * Originating ad-hoc group chat session
 * 
 * @author jexa7410
 */
public class OriginatingAdhocGroupChatSession extends GroupChatSession {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    private static final Logger sLogger = Logger.getLogger(OriginatingAdhocGroupChatSession.class
            .getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param conferenceId Conference ID
     * @param subject Subject associated to the session
     * @param participantsToInvite Map of participants to invite
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager the contact manager
     * @param cmsManager the CMS manager
     */
    public OriginatingAdhocGroupChatSession(InstantMessagingService imService, Uri conferenceId,
            String subject, Map<ContactId, ParticipantStatus> participantsToInvite,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager, CmsManager cmsManager) {
        super(imService, null, conferenceId, participantsToInvite, rcsSettings, messagingLog,
                timestamp, contactManager, cmsManager);

        if (!TextUtils.isEmpty(subject)) {
            setSubject(subject);
        }

        createOriginatingDialogPath();

        String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
        setContributionID(id);
    }

    @Override
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new ad-hoc group chat session as originating");
            }

            String localSetup = createSetupOffer();
            if (sLogger.isActivated()) {
                sLogger.debug("Local setup attribute is ".concat(localSetup));
            }

            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; /* See RFC4145, Page 4 */
            } else {
                localMsrpPort = getMsrpMgr().getLocalMsrpPort();
            }

            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildGroupChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
                    .getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
                    getMsrpMgr().getLocalMsrpPath(), SdpUtils.DIRECTION_SENDRECV);

            String resourceList = ChatUtils.generateChatResourceList(getParticipants().keySet());

            String multipart = Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF
                    + "Content-Type: application/sdp" + SipUtils.CRLF + "Content-Length: "
                    + sdp.getBytes(UTF8).length + SipUtils.CRLF + SipUtils.CRLF + sdp
                    + SipUtils.CRLF + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG + SipUtils.CRLF
                    + "Content-Type: application/resource-lists+xml" + SipUtils.CRLF
                    + "Content-Length: " + resourceList.getBytes(UTF8).length + SipUtils.CRLF
                    + "Content-Disposition: recipient-list" + SipUtils.CRLF + SipUtils.CRLF
                    + resourceList + SipUtils.CRLF + Multipart.BOUNDARY_DELIMITER + BOUNDARY_TAG
                    + Multipart.BOUNDARY_DELIMITER;

            getDialogPath().setLocalContent(multipart);

            if (sLogger.isActivated()) {
                sLogger.info("Send INVITE");
            }
            SipRequest invite = createInviteRequest(multipart);

            getAuthenticationAgent().setAuthorizationHeader(invite);

            getDialogPath().setInvite(invite);

            sendInvite(invite);

        } catch (InvalidArgumentException | RuntimeException | NetworkException | PayloadException
                | FileAccessException | ParseException e) {
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        }
    }

    /**
     * Create INVITE request
     * 
     * @param content Content part
     * @return Request
     * @throws PayloadException
     */
    private SipRequest createInviteRequest(String content) throws PayloadException {
        try {
            SipRequest invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
                    getFeatureTags(), getAcceptContactTags(), content, BOUNDARY_TAG);
            if (getSubject() != null) {
                invite.addHeader(SubjectHeader.NAME, getSubject());
            }
            invite.addHeader(RequireHeader.NAME, "recipient-list-invite");
            invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            return invite;

        } catch (ParseException e) {
            throw new PayloadException("Failed to create invite request!", e);
        }
    }

    @Override
    public SipRequest createInvite() throws PayloadException {
        return createInviteRequest(getDialogPath().getLocalContent());
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    @Override
    public void startSession() {
        getImsService().getImsModule().getInstantMessagingService().addSession(this);
        start();
    }
}
