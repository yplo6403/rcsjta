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

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.EventReportingFrameworkConfig;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the entry point for the event framework.<br>
 * It allows:
 * <ul>
 * <li>- to push XMS messages on the message store with IMAP commands</li>
 * <li>- to update flags of messages on the message store with SIP or IMAP commands The protocol
 * used for updating flags depends on provisioning parameters.</li>
 * </ul>
 */
public class EventFrameworkManager implements IEventFrameworkListener {

    public final static String MIME_TYPE = "application/vnd.oma.cpm-eventfw+xml";

    private static final Logger sLogger = Logger.getLogger(EventFrameworkManager.class
            .getSimpleName());

    private final RcsSettings mSettings;
    private final InstantMessagingService mInstantMessagingService;
    private final CmsLog mCmsLog;
    private final CmsSyncScheduler mCmsSyncScheduler;

    /**
     * Constructor
     * 
     * @param scheduler the scheduler
     * @param instantMessagingService the IMS module
     * @param cmsLog the CMS log accessor
     * @param settings the RCS settings accessor
     */
    public EventFrameworkManager(CmsSyncScheduler scheduler,
            InstantMessagingService instantMessagingService, CmsLog cmsLog, RcsSettings settings) {
        mInstantMessagingService = instantMessagingService;
        mCmsSyncScheduler = scheduler;
        mSettings = settings;
        mCmsLog = cmsLog;
    }

    @Override
    public void updateFlagsForXms(ContactId contact) {
        updateFlags(contact, null);
    }

    @Override
    public void updateFlagsForChat(ContactId contact) {
        updateFlags(contact, null);
    }

    @Override
    public void updateFlagsForGroupChat(String chatId) {
        updateFlags(null, chatId);
    }

    private SipEventFrameworkDocument buildDocument(String cmsFolder, String contributionId) {
        List<CmsObject> seenObjects = new ArrayList<>();
        List<CmsObject> deletedObjects = new ArrayList<>();
        for (CmsObject cmsObject : mCmsLog.getMessagesToSync(cmsFolder)) {
            if (CmsObject.ReadStatus.READ_REPORT_REQUESTED == cmsObject.getReadStatus()) {
                seenObjects.add(cmsObject);
            }
            if (CmsObject.DeleteStatus.DELETED_REPORT_REQUESTED == cmsObject.getDeleteStatus()) {
                deletedObjects.add(cmsObject);
            }
        }
        if (seenObjects.isEmpty() && deletedObjects.isEmpty()) {
            return null;
        }
        return new SipEventFrameworkDocument(seenObjects, deletedObjects, contributionId);
    }

    private void updateFlags(ContactId contactId, String chatId) {
        EventReportingFrameworkConfig cfg = mSettings.getEventReportingFrameworkConfig();
        if (EventReportingFrameworkConfig.DISABLED == cfg) {
            if (sLogger.isActivated()) {
                sLogger.debug("Event reporting framework is not enabled");
            }
            return;
        }
        boolean isOneToOne = contactId != null;
        /*
         * When enabled, try to report events with an established MSRP session first and with an
         * IMAP session otherwise.
         */
        switch (cfg) {
            case ENABLED:
                ChatSession chatSession;
                String cmsFolder;
                String contributionId = null;
                if (isOneToOne) {
                    chatSession = mInstantMessagingService.getOneToOneChatSession(contactId);
                    cmsFolder = CmsUtils.contactToCmsFolder(contactId);
                } else {
                    /*
                     * For GC, the contribution ID is equals to the chat ID and is persisted. The
                     * contribution ID is never null (there is no forward session).
                     */
                    contributionId = chatId;
                    chatSession = mInstantMessagingService.getGroupChatSession(chatId);
                    cmsFolder = CmsUtils.groupChatToCmsFolder(chatId, chatId);
                }
                if (chatSession != null && chatSession.isMediaEstablished()) {
                    /*
                     * Event framework notification is sent in the MSRP session.
                     */
                    if (contributionId == null) {
                        /*
                         * For 1 to 1 chat, the contribution ID is not persisted but can be
                         * retrieved from the session. Since the MSRP is established, it is not a
                         * forward session and then contribution ID is not null.
                         */
                        contributionId = chatSession.getContributionID();
                    }
                    SipEventFrameworkDocument sipEventReportingFrameworkDoc = buildDocument(
                            cmsFolder, contributionId);
                    if (sipEventReportingFrameworkDoc != null) {
                        byte[] bytes = sipEventReportingFrameworkDoc.toXml().getBytes(UTF8);
                        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                        try {
                            chatSession.getMsrpMgr().sendChunks(stream,
                                    IdGenerator.generateMessageID(), MIME_TYPE, bytes.length,
                                    MsrpSession.TypeMsrpChunk.EventReportingFramework);

                        } catch (NetworkException e) {
                            if (sLogger.isActivated()) {
                                if (isOneToOne) {
                                    sLogger.warn("Failed to update flags for contact " + contactId,
                                            e);
                                } else {
                                    sLogger.warn("Failed to update flagsfor chatId " + chatId, e);
                                }
                            }
                        }
                    } else {
                        if (isOneToOne) {
                            sLogger.error("Cannot update flags, xml document is null for contact "
                                    + contactId);
                        } else {
                            sLogger.error("Cannot update flags, xml document is null for chatId "
                                    + chatId);
                        }
                    }
                } else { // no MSRP session available, use IMAP session instead
                    if (isOneToOne) {
                        updateFlags(contactId);
                    } else {
                        updateFlags(chatId);
                    }
                }
                break;

            case IMAP_ONLY:
                if (isOneToOne) {
                    updateFlags(contactId);
                } else {
                    updateFlags(chatId);
                }
                break;
        }
    }

    private void updateFlags(ContactId contact) {
        if (!mCmsSyncScheduler.scheduleUpdateFlags(contact)) {
            if (sLogger.isActivated()) {
                sLogger.info("--> can not schedule update flag operation for contact : "
                        + contact.toString());
            }
        }
    }

    private void updateFlags(String chatId) {
        if (!mCmsSyncScheduler.scheduleUpdateFlags(chatId)) {
            if (sLogger.isActivated()) {
                sLogger.info("--> can not schedule update flag operation for chatId : " + chatId);
            }
        }
    }

}
