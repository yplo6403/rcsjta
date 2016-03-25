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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsObject.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsObject.ReadStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.FifoBuffer;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class SipEventReportingFrameworkManager extends Thread {

    public final static String MIME_TYPE = "application/vnd.oma.cpm-eventfw+xml";

    private FifoBuffer mBuffer = new FifoBuffer();

    private final RcsSettings mRcsSettings;
    private final CmsLog mCmsLog;

    private final static Logger sLogger = Logger.getLogger(SipEventReportingFrameworkManager.class
            .getSimpleName());

    /**
     * Constructor
     *
     * @param rcsSettings
     * @param cmsLog
     */
    public SipEventReportingFrameworkManager(RcsSettings rcsSettings, CmsLog cmsLog) {
        mRcsSettings = rcsSettings;
        mCmsLog = cmsLog;
    }

    /**
     * Terminate manager
     */
    public void terminate() {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the SIP Event Framework manager");
        }
        mBuffer.close();
    }

    /**
     * Background processing
     */
    public void run() {
        SipEventReporting sipEventReporting;
        while ((sipEventReporting = (SipEventReporting) mBuffer.getObject()) != null) {
            try {
                ChatSession session = sipEventReporting.mSession;
                if (session != null && session.isMediaEstablished()) {
                    byte[] bytes = sipEventReporting.mDocument.toXml().getBytes(UTF8);
                    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                    session.getMsrpMgr().sendChunks(stream, IdGenerator.generateMessageID(),
                            MIME_TYPE, bytes.length, TypeMsrpChunk.EventReportingFramework);

                    // TODO FGI : the result of this operation should be send with IMDN positive /
                    // negative delivery
                    // TODO FGI : check the result from participating function
                    // (MsrpEventListener::msrpDataTransferred)

                    // TODO FGI : update cmsLog entries : READ_REPORT_REQUESTED --> READ
                    // TODO FGI : update cmsLog entries : DELETED_REPORT_REQUESTED --> DELETED
                }
            } catch (NetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
            } catch (RuntimeException e) {
                /*
                 * Intentionally catch runtime exceptions as else it will abruptly end the thread
                 * and eventually bring the whole system down, which is not intended.
                 */
                sLogger.error("Failed to send SipReportingEvents", e);
            }
        }
    }

    public void tryToReportEvents(String cmsFolder, ChatSession chatSession) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("--> try to report events for Cms folder : " + cmsFolder);
        }
        if (chatSession == null || !chatSession.isMediaEstablished()) {
            if (logActivated) {
                sLogger.debug(" There is no active MSRP ChatSession");
                sLogger.debug(" --> Can not report events ");
            }
            return;
        }

        SipEventReportingFrameworkDocument doc = buildDocument(cmsFolder);
        if (doc == null) { // nothing to report
            if (sLogger.isActivated()) {
                sLogger.debug(" --> Nothing to report, xml document is null");
            }
            return;
        }

        mBuffer.addObject(new SipEventReporting(doc, chatSession));
    }

    private SipEventReportingFrameworkDocument buildDocument(String cmsFolder) {

        List<CmsObject> seenObjects = new ArrayList<>();
        List<CmsObject> deletedObjects = new ArrayList<>();
        for (CmsObject cmsObject : mCmsLog.getMessagesToSync(cmsFolder)) {
            if (ReadStatus.READ_REPORT_REQUESTED == cmsObject.getReadStatus()) {
                seenObjects.add(cmsObject);
            }
            if (DeleteStatus.DELETED_REPORT_REQUESTED == cmsObject.getDeleteStatus()) {
                deletedObjects.add(cmsObject);
            }
        }

        if (seenObjects.isEmpty() && deletedObjects.isEmpty()) {
            return null;
        }

        return new SipEventReportingFrameworkDocument(seenObjects, deletedObjects);
    }

    /**
     * Sip event reporting
     */
    private static class SipEventReporting {

        final SipEventReportingFrameworkDocument mDocument;
        final ChatSession mSession;

        SipEventReporting(SipEventReportingFrameworkDocument document, ChatSession session) {
            mDocument = document;
            mSession = session;
        }
    }
}
