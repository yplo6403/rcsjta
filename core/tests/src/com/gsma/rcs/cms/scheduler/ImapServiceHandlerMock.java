package com.gsma.rcs.cms.scheduler;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceHandler;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

public class ImapServiceHandlerMock extends ImapServiceHandler {

    private static final Logger sLogger = Logger.getLogger(ImapServiceHandlerMock.class.getSimpleName());

    public ImapServiceHandlerMock(RcsSettings rcsSettings) {
        super(rcsSettings);
    }

    public synchronized BasicImapService openService() {
        if (sLogger.isActivated()) {
            sLogger.debug("--> open mock IMAP Service");
        }
        return null;
    }

    /**
     * Close the current service (connection) with the CMS server
     */
    public synchronized void closeService() {
        if (sLogger.isActivated()) {
            sLogger.debug("<-- close mock IMAP Service");
        }
    }
}
