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
 ******************************************************************************/

package com.gsma.rcs.core.cms.sync.scheduler;

import com.gsma.rcs.core.cms.protocol.service.BasicImapService;
import com.gsma.rcs.core.cms.protocol.service.ImapServiceHandler;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

public class ImapServiceHandlerMock extends ImapServiceHandler {

    private static final Logger sLogger = Logger.getLogger(ImapServiceHandlerMock.class
            .getSimpleName());

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
