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

package com.gsma.rcs.core.cms.sync.scheduler;

import android.util.SparseArray;

/**
 * enum type to define synchronization scheduler task type
 */
public enum CmsSyncSchedulerTaskType {

    SYNC_FOR_USER_ACTIVITY(0), SYNC_PERIODIC(1), SYNC_FOR_DATA_CONNECTION(2), PUSH_MESSAGES(3), UPDATE_FLAGS(
            4);

    private int mValue;

    private static SparseArray<CmsSyncSchedulerTaskType> mValueToEnum = new SparseArray<>();

    static {
        for (CmsSyncSchedulerTaskType entry : CmsSyncSchedulerTaskType.values()) {
            mValueToEnum.put(entry.toInt(), entry);
        }
    }

    CmsSyncSchedulerTaskType(int value) {
        mValue = value;
    }

    /**
     * Gets integer value associated to MessagingMode instance
     *
     * @return value
     */
    public final int toInt() {
        return mValue;
    }

    /**
     * Returns a SyncTrigger instance for the specified integer value.
     *
     * @param value the value representing hte scheduler type
     * @return instance
     */
    public static CmsSyncSchedulerTaskType valueOf(int value) {
        CmsSyncSchedulerTaskType entry = mValueToEnum.get(value);
        if (entry != null) {
            return entry;
        }
        throw new IllegalArgumentException("No enum const class " +
                CmsSyncSchedulerTaskType.class.getName() + "" + value);
    }
}