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

package com.orangelabs.rcs.ri.messaging;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import com.orangelabs.rcs.ri.messaging.adapter.OneToOneTalkArrayItem;
import com.orangelabs.rcs.ri.utils.ContactUtil;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to update the one to one talk list in background
 *
 * @author Philippe LEMORDANT
 */
public class OneToOneTalkListUpdate extends
        AsyncTask<Void, Void, Collection<OneToOneTalkArrayItem>> {

    // @formatter:off
    private static final String[] PROJECTION = new String[]{
            HistoryLog.BASECOLUMN_ID,
            HistoryLog.ID,
            HistoryLog.PROVIDER_ID,
            HistoryLog.MIME_TYPE,
            HistoryLog.CONTENT,
            HistoryLog.TIMESTAMP,
            HistoryLog.DIRECTION,
            HistoryLog.CONTACT,
            HistoryLog.READ_STATUS
    };
    // @formatter:on

    private static final String WHERE_CLAUSE = HistoryLog.CHAT_ID + "=" + HistoryLog.CONTACT;

    private final TaskCompleted mTaskCompleted;
    private final Context mCtx;

    public OneToOneTalkListUpdate(Context ctx, TaskCompleted taskCompleted) {
        mCtx = ctx;
        mTaskCompleted = taskCompleted;
    }

    @Override
    protected Collection<OneToOneTalkArrayItem> doInBackground(Void... params) {
        /*
         * The MMS sending is performed in background because the API returns a message instance
         * only once it is persisted and to persist MMS, the core stack computes the file icon for
         * image attached files.
         */
        return queryHistoryLogAndRefreshView();
    }

    @Override
    protected void onPostExecute(Collection<OneToOneTalkArrayItem> result) {
        if (mTaskCompleted != null) {
            mTaskCompleted.onTaskComplete(result);
        }
    }

    public interface TaskCompleted {
        void onTaskComplete(Collection<OneToOneTalkArrayItem> result);
    }

    Collection<OneToOneTalkArrayItem> queryHistoryLogAndRefreshView() {
        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(XmsMessageLog.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        Uri mUriHistoryProvider = uriBuilder.build();
        Map<ContactId, OneToOneTalkArrayItem> dataMap = new HashMap<>();
        Cursor cursor = null;
        try {
            cursor = mCtx.getContentResolver().query(mUriHistoryProvider, PROJECTION, WHERE_CLAUSE,
                    null, null);
            if (cursor == null) {
                throw new IllegalStateException("Cannot query History Log");
            }
            int columnTimestamp = cursor.getColumnIndexOrThrow(HistoryLog.TIMESTAMP);
            int columnProviderId = cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID);
            int columnDirection = cursor.getColumnIndexOrThrow(HistoryLog.DIRECTION);
            int columnContact = cursor.getColumnIndexOrThrow(HistoryLog.CONTACT);
            int columnContent = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
            int columnMimeType = cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE);
            int columnReadStatus = cursor.getColumnIndexOrThrow(HistoryLog.READ_STATUS);
            while (cursor.moveToNext()) {
                long timestamp = cursor.getLong(columnTimestamp);
                String phoneNumber = cursor.getString(columnContact);
                ContactId contact = ContactUtil.formatContact(phoneNumber);
                RcsService.Direction dir = RcsService.Direction.valueOf(cursor
                        .getInt(columnDirection));
                RcsService.ReadStatus readStatus = RcsService.ReadStatus.valueOf(cursor
                        .getInt(columnReadStatus));
                OneToOneTalkArrayItem item = dataMap.get(contact);
                int unreadCount = (RcsService.Direction.INCOMING == dir && RcsService.ReadStatus.UNREAD == readStatus) ? 1
                        : 0;
                if (item != null) {
                    if (timestamp < item.getTimestamp()) {
                        if (RcsService.Direction.INCOMING == dir
                                && RcsService.ReadStatus.UNREAD == readStatus) {
                            item.incrementUnreadCount();
                        }
                        continue;
                    }
                    unreadCount += item.getUnreadCount();

                }
                int providerId = cursor.getInt(columnProviderId);
                String content = null;
                if (FileTransferLog.HISTORYLOG_MEMBER_ID != providerId) {
                    /* There is not body text message for RCS file transfer */
                    content = cursor.getString(columnContent);
                }
                String mimeType = cursor.getString(columnMimeType);
                dataMap.put(contact, new OneToOneTalkArrayItem(contact, timestamp, dir, content,
                        mimeType, unreadCount));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return dataMap.values();
    }

}
