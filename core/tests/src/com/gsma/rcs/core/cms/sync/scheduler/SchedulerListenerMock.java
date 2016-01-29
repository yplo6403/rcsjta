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

import com.gsma.rcs.core.cms.sync.scheduler.Scheduler.SyncType;
import com.gsma.rcs.utils.logger.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulerListenerMock implements SchedulerListener {

    private static final Logger sLogger = Logger.getLogger(SchedulerListenerMock.class
            .getSimpleName());

    private Map<SchedulerTaskType, AtomicInteger> executions;

    SchedulerListenerMock() {
        executions = new HashMap<>();
    }

    @Override
    public void onCmsOperationExecuted(SchedulerTaskType operation, SyncType syncType, boolean result,
            Object param) {
        sLogger.info("onCmsOperationExecuted " + operation);
        AtomicInteger nb = executions.get(operation);
        if (nb == null) {
            nb = new AtomicInteger(0);
        }
        nb.incrementAndGet();
        executions.put(operation, nb);
    }

    public int getExecutions(SchedulerTaskType schedulerTaskType) {
        return executions.get(schedulerTaskType) == null ? 0 : executions.get(schedulerTaskType).intValue();
    }
}
