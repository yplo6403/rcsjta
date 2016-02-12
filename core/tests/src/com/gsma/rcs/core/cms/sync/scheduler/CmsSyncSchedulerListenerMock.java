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

import com.gsma.rcs.core.cms.sync.scheduler.CmsSyncScheduler.SyncType;
import com.gsma.rcs.utils.logger.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CmsSyncSchedulerListenerMock implements CmsSyncSchedulerListener {

    private static final Logger sLogger = Logger.getLogger(CmsSyncSchedulerListenerMock.class
            .getSimpleName());

    private Map<CmsSyncSchedulerTaskType, AtomicInteger> executions;

    CmsSyncSchedulerListenerMock() {
        executions = new HashMap<>();
    }

    @Override
    public void onCmsOperationExecuted(CmsSyncSchedulerTaskType operation, SyncType syncType, boolean result,
                                       Object param) {
        sLogger.info("onCmsOperationExecuted " + operation);
        AtomicInteger nb = executions.get(operation);
        if (nb == null) {
            nb = new AtomicInteger(0);
        }
        nb.incrementAndGet();
        executions.put(operation, nb);
    }

    public int getExecutions(CmsSyncSchedulerTaskType schedulerTaskType) {
        return executions.get(schedulerTaskType) == null ? 0 : executions.get(schedulerTaskType).intValue();
    }
}
