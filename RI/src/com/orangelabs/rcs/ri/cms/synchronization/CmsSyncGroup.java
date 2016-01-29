/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.ri.cms.synchronization;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.utils.RcsFragmentActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LogUtils;

import java.util.Map;

/**
 * Synchronize group chats
 *
 * @author Philippe LEMORDANT
 */
public class CmsSyncGroup extends RcsFragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String SORT_ORDER = ChatLog.GroupChat.TIMESTAMP + " DESC";

    private static final String LOGTAG = LogUtils.getTag(CmsSyncGroup.class.getSimpleName());
    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;
    // @formatter:off
    String[] PROJECTION = new String[]{
            ChatLog.GroupChat.BASECOLUMN_ID,
            ChatLog.GroupChat.CHAT_ID,
            ChatLog.GroupChat.SUBJECT,
            ChatLog.GroupChat.STATE,
            ChatLog.GroupChat.REASON_CODE,
            ChatLog.GroupChat.TIMESTAMP,
            ChatLog.GroupChat.PARTICIPANTS
    };
    // @formatter:on

    private ListView mListView;
    private SyncGroupListAdapter mAdapter;
    private CmsService mCmsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_list);

        /* Set list adapter */
        mListView = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyView);
        registerForContextMenu(mListView);

        mAdapter = new SyncGroupListAdapter(this);
        mListView.setAdapter(mAdapter);
        /*
         * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
         */
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

        /* Register to API connection manager */
        if (!isServiceConnected(ConnectionManager.RcsServiceName.CMS)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(ConnectionManager.RcsServiceName.CMS);
        mCmsService = getCmsApi();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_log_cms_sync_group_item, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /* Get selected item */
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mListView.getAdapter()).getItem(info.position);
        String chatId = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.CHAT_ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected chatId=".concat(chatId));
        }
        switch (item.getItemId()) {
            case R.id.menu_log_cms_sync_group_item:
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Synchronize group chat for chatId=".concat(chatId));
                }
                try {
                    mCmsService.syncGroupConversation(chatId);
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, ChatLog.GroupChat.CONTENT_URI, PROJECTION, null, null,
                SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        /* A switch-case is useful when dealing with multiple Loaders/IDs */
        switch (loader.getId()) {
            case LOADER_ID:
                /*
                 * The asynchronous load is complete and the data is now available for use. Only now
                 * can we associate the queried Cursor with the CursorAdapter.
                 */
                mAdapter.swapCursor(cursor);
                break;
        }
        /* The listview now displays the queried data. */
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * For whatever reason, the Loader's data is now unavailable. Remove any references to the
         * old data by replacing it with a null Cursor.
         */
        mAdapter.swapCursor(null);
    }

    /**
     * CMS sync group list adapter
     */
    private class SyncGroupListAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        /**
         * Constructor
         *
         * @param context Context
         */
        public SyncGroupListAdapter(Context context) {
            super(context, null, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mInflater.inflate(R.layout.chat_group_list_item, parent, false);
            view.setTag(new GroupChatListItemViewHolder(view, cursor));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final GroupChatListItemViewHolder holder = (GroupChatListItemViewHolder) view.getTag();
            long date = cursor.getLong(holder.columnDate);
            holder.dateText
                    .setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));

            String subject = cursor.getString(holder.columnSubject);
            if (TextUtils.isEmpty(subject)) {
                holder.subjectText.setText(context.getString(R.string.label_subject_notif,
                        "<" + context.getString(R.string.label_no_subject) + ">"));
            } else {
                holder.subjectText
                        .setText(context.getString(R.string.label_subject_notif, subject));
            }
            int state = cursor.getInt(holder.columnState);
            if (state < RiApplication.sGroupChatStates.length) {
                holder.stateText.setText(RiApplication.sGroupChatStates[state]);
            }
            int reason = cursor.getInt(holder.columnReasonCode);
            if (reason < RiApplication.sGroupChatReasonCodes.length) {
                if (GroupChat.ReasonCode.UNSPECIFIED == GroupChat.ReasonCode.valueOf(reason)) {
                    holder.reasonText.setVisibility(View.GONE);
                } else {
                    holder.reasonText.setVisibility(View.VISIBLE);
                    holder.reasonText.setText(RiApplication.sGroupChatReasonCodes[reason]);
                }
            }

            try {
                Map<ContactId, ParticipantStatus> mapOfparticipants = ChatLog.GroupChat
                        .getParticipants(CmsSyncGroup.this,
                                cursor.getString(holder.columnParticipants));
                StringBuilder sb = null;
                for (ContactId participant : mapOfparticipants.keySet()) {
                    if (sb == null) {
                        sb = new StringBuilder(participant.toString());
                    } else {
                        sb.append("\n");
                        sb.append(participant.toString());
                    }
                }
                if (sb != null) {
                    holder.participantsText.setText(sb.toString());
                }
            } catch (RcsPermissionDeniedException e) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "getParticipants failed", e);
                }
            }

        }
    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById() or getColumnIndex() on each row.
     */
    private class GroupChatListItemViewHolder {
        TextView subjectText;

        TextView dateText;

        TextView stateText;

        TextView reasonText;

        int columnSubject;

        TextView participantsText;

        int columnDate;

        int columnState;

        int columnParticipants;

        int columnReasonCode;

        GroupChatListItemViewHolder(View base, Cursor cursor) {
            columnSubject = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT);
            columnDate = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.TIMESTAMP);
            columnState = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.STATE);
            columnParticipants = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.PARTICIPANTS);
            columnReasonCode = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.REASON_CODE);
            subjectText = (TextView) base.findViewById(R.id.subject);
            dateText = (TextView) base.findViewById(R.id.date);
            stateText = (TextView) base.findViewById(R.id.state);
            participantsText = (TextView) base.findViewById(R.id.participants);
            reasonText = (TextView) base.findViewById(R.id.reason);
        }
    }
}
