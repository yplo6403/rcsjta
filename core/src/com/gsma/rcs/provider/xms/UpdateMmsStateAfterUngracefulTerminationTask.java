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

package com.gsma.rcs.provider.xms;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;

/**
 * Created by yplo6403 on 08/12/2015.
 */
public class UpdateMmsStateAfterUngracefulTerminationTask implements Runnable {

    private static final Logger sLogger = Logger
            .getLogger(UpdateMmsStateAfterUngracefulTerminationTask.class.getName());
    private final XmsLog mXmsLog;
    private final CmsServiceImpl mCmsServiceImpl;

    public UpdateMmsStateAfterUngracefulTerminationTask(XmsLog xmsLog, CmsServiceImpl cmsServiceImpl) {
        mXmsLog = xmsLog;
        mCmsServiceImpl = cmsServiceImpl;
    }

    @Override
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.debug("UpdateMmsStateAfterUngracefulTerminationTask");
            Cursor cursor = null;
            try {
                cursor = mXmsLog.getInterruptedMmsTransfers();
                int contactIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_CONTACT);
                int mmsIdIdx = cursor.getColumnIndexOrThrow(XmsData.KEY_MESSAGE_ID);
                while (cursor.moveToNext()) {
                    String mmsId = cursor.getString(mmsIdIdx);
                    String contactNumber = cursor.getString(contactIdx);
                    ContactId contact = ContactUtil.createContactIdFromTrustedData(contactNumber);
                    mCmsServiceImpl.setXmsStateAndReasonCode(mmsId,
                            XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE, contact,
                            XmsMessage.State.FAILED, XmsMessage.ReasonCode.UNSPECIFIED);
                }
            } catch (RuntimeException e) {
                /*
                 * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
                 * which should be handled/fixed within the code. However the cases when we are
                 * executing operations on a thread unhandling such exceptions will eventually lead
                 * to exit the system and thus can bring the whole system down, which is not
                 * intended.
                 */
                sLogger.error(
                        "Exception occurred while trying to update MMS state for interrupted transfers",
                        e);
            } finally {
                CursorUtil.close(cursor);
                if (sLogger.isActivated()) {
                    sLogger.debug("done.");
                }
            }
        }
    }
}
