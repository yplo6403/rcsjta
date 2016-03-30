/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.ri.cms.messaging;

import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.os.AsyncTask;

import java.util.List;

/**
 * A class to send MMS in background
 *
 * @author Philippe LEMORDANT
 */
public class SendMmsInBackground extends AsyncTask<Void, Void, Exception> {
    private final CmsService mCmsService;
    private final ContactId mContact;
    private final List<Uri> mFiles;
    private final String mSubject;
    private final String mBody;
    private final TaskCompleted mTaskCompleted;

    public SendMmsInBackground(CmsService cmsService, ContactId contact, List<Uri> files,
            String subject, String body, TaskCompleted taskCompleted) {
        mCmsService = cmsService;
        mContact = contact;
        mFiles = files;
        mSubject = subject;
        mBody = body;
        mTaskCompleted = taskCompleted;
    }

    @Override
    protected Exception doInBackground(Void... params) {
        /*
         * The MMS sending is performed in background because the API returns a message instance
         * only once it is persisted and to persist MMS, the core stack computes the file icon for
         * image attached files.
         */
        try {
            mCmsService.sendMultimediaMessage(mContact, mFiles, mSubject, mBody);
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    @Override
    protected void onPostExecute(Exception result) {
        if (mTaskCompleted != null) {
            mTaskCompleted.onTaskComplete(result);
        }
    }

    public interface TaskCompleted {
        void onTaskComplete(Exception result);
    }

}
