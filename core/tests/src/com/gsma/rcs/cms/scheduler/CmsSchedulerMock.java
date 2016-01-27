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

package com.gsma.rcs.cms.scheduler;

import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.imap.service.ImapServiceHandler;
import com.gsma.rcs.cms.imap.task.CmsTask;
import com.gsma.rcs.cms.provider.imap.ImapLog;
import com.gsma.rcs.cms.storage.LocalStorage;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

public class CmsSchedulerMock extends CmsScheduler {

    private static final Logger sLogger = Logger.getLogger(CmsSchedulerMock.class.getSimpleName());

    private long mExecutionDuration = 100; // in ms

    public CmsSchedulerMock(Context context, RcsSettings rcsSettings, LocalStorage localStorage,
            ImapLog imapLog, XmsLog xmsLog) {
        super(context, rcsSettings, localStorage, imapLog, xmsLog);
    }

    void executeSync(BasicImapService basicImapService, SyncParams syncParams) {
        sLogger.info("executeSync");
        try {
            Thread.sleep(mExecutionDuration, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void executePush(BasicImapService basicImapService, ContactId contact) {
        sLogger.info("executePush");
        try {
            Thread.sleep(mExecutionDuration, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void executeUpdate(BasicImapService basicImapService) {
        sLogger.info("executeUpdate");
        try {
            Thread.sleep(mExecutionDuration, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void executeCmsTask(BasicImapService basicImapService, CmsTask cmsTask) {
        sLogger.info("executeCmsTask");
        try {
            Thread.sleep(mExecutionDuration, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void setExecutionDuration(int executionDuration) {
        mExecutionDuration = executionDuration;
    }

    void setImapServiceHandler(ImapServiceHandler imapServiceHandler) {
        mImapServiceHandler = imapServiceHandler;
    }
}
