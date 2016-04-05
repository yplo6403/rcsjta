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
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.SessionActivityManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Event Reporting session
 */
public abstract class EventFrameworkSession extends ImsServiceSession implements MsrpEventListener {

    private final MsrpManager mMsrpMgr;

    private final SessionActivityManager mActivityMgr;

    private String mContributionId;

    private List<String> mFeatureTags = new ArrayList<>();

    private String mAcceptTypes = "";

    private String mWrappedTypes = "";

    private static final Logger sLogger = Logger.getLogger(EventFrameworkSession.class.getName());

    protected final MessagingLog mMessagingLog;

    protected final InstantMessagingService mImService;

    /**
     * Constructor
     *
     * @param imService InstantMessagingService
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     */
    public EventFrameworkSession(InstantMessagingService imService, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp) {
        super(imService, null, null, rcsSettings, timestamp, null);

        mImService = imService;
        mMessagingLog = messagingLog;
        mActivityMgr = new SessionActivityManager(this, rcsSettings);

        addAcceptTypes(EventFrameworkManager.MIME_TYPE);
        addWrappedTypes(CpimMessage.MIME_TYPE);

        mFeatureTags.add(FeatureTags.FEATURE_3GPP + "=\""
                + FeatureTags.FEATURE_3GPP_SERVICE_CPM_SYSTEM_MSG + "\"");

        // Create the MSRP manager
        int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(rcsSettings);
        String localIpAddress = mImService.getImsModule().getCurrentNetworkInterface()
                .getNetworkAccess().getIpAddress();
        mMsrpMgr = new MsrpManager(localIpAddress, localMsrpPort, imService, rcsSettings);
        if (imService.getImsModule().isConnectedToWifiAccess()) {
            mMsrpMgr.setSecured(rcsSettings.isSecureMsrpOverWifi());
        }
    }

    /**
     * Get feature tags
     *
     * @return Feature tags
     */
    public String[] getFeatureTags() {
        return mFeatureTags.toArray(new String[mFeatureTags.size()]);
    }

    /**
     * Get accept types
     *
     * @return Accept types
     */
    public String getAcceptTypes() {
        return mAcceptTypes;
    }

    /**
     * Add types to accept types
     * 
     * @param types types
     */
    public void addAcceptTypes(String types) {
        if (mAcceptTypes.isEmpty()) {
            mAcceptTypes += types;
        } else {
            mAcceptTypes += " " + types;
        }
    }

    /**
     * Get wrapped types
     *
     * @return Wrapped types
     */
    public String getWrappedTypes() {
        return mWrappedTypes;
    }

    /**
     * Add types to wrapped types
     * 
     * @param types types
     */
    public void addWrappedTypes(String types) {
        if (mWrappedTypes.isEmpty()) {
            mWrappedTypes += types;
        } else {
            mWrappedTypes += " " + types;
        }
    }

    /**
     * Returns the session activity manager
     *
     * @return Activity manager
     */
    public SessionActivityManager getActivityManager() {
        return mActivityMgr;
    }

    /**
     * Return the contribution ID
     *
     * @return Contribution ID
     */
    public String getContributionID() {
        return mContributionId;
    }

    /**
     * Set the contribution ID
     *
     * @param id Contribution ID
     */
    public void setContributionID(String id) {
        mContributionId = id;
    }

    /**
     * Returns the MSRP manager
     *
     * @return MSRP manager
     */
    public MsrpManager getMsrpMgr() {
        return mMsrpMgr;
    }

    /**
     * Close the MSRP session
     */
    public void closeMsrpSession() {
        if (getMsrpMgr() != null) {
            getMsrpMgr().closeSession();
            if (sLogger.isActivated()) {
                sLogger.debug("MSRP session has been closed");
            }
        }
    }

    @Override
    public void handle480Unavailable(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED, resp.getReasonPhrase()));
    }

    @Override
    public void handle486Busy(SipResponse resp) {
        handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED, resp.getReasonPhrase()));
    }

    @Override
    public void handle603Declined(SipResponse resp) {
        handleDefaultError(resp);
    }

    @Override
    public void handleError(ImsServiceError error) {
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    @Override
    public void msrpDataTransferred(String msgId) {
        if (sLogger.isActivated()) {
            sLogger.info("Data transferred");
        }
        mActivityMgr.updateActivity();
    }

    @Override
    public void handleInactivityEvent() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Session inactivity event");
        }
        terminateSession(TerminationReason.TERMINATION_BY_INACTIVITY);
    }

    @Override
    public void receiveMsrpData(String msgId, byte[] data, String mimeType)
            throws PayloadException, NetworkException, ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.info("Data received (type " + mimeType + ")");
        }
        mActivityMgr.updateActivity();
        // TODO check if it is necessary to test if data is empty ?
        if (data == null || data.length == 0) {
            if (sLogger.isActivated()) {
                sLogger.debug("By-pass received empty data");
            }
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("EventFrameworkSession::receiveMsrpData");
        }
        // TODO FGI : TO BE implemented
        /*
         * For positive AS ack, update flag report status in local storage. DELETED_REPORT_REQUESTED
         * --> DELETED READ_REPORT_REQUESTED --> READ
         */
    }

    @Override
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used by chat
    }

    @Override
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used by chat
        return false;
    }

    @Override
    public void msrpTransferAborted() {
    }

    @Override
    public void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk) {
    }

    public void sendEmptyDataChunk() throws NetworkException {
        mMsrpMgr.sendEmptyChunk();
    }

    @Override
    public void startSession() throws PayloadException, NetworkException {
    }

    @Override
    public void removeSession() {
    }

    @Override
    public void prepareMediaSession() {
        byte[] sdp = getDialogPath().getRemoteContent().getBytes(UTF8);
        MsrpSession session = getMsrpMgr().createMsrpSession(sdp, this);
        session.setFailureReportOption(false);
        session.setSuccessReportOption(false);
    }

    @Override
    public void openMediaSession() throws PayloadException, NetworkException {
        getMsrpMgr().openMsrpSession();
        sendEmptyDataChunk();
    }

    @Override
    public void startMediaTransfer() {
    }

    @Override
    public void closeMediaSession() {
        getActivityManager().stop();
        closeMsrpSession();
    }

    @Override
    public SipRequest createInvite() throws PayloadException {
        return null;
    }

}
