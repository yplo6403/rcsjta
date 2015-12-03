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

package com.orangelabs.rcs.ri.cms.messaging;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageListener;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsFragmentActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.adapter.TalkCursorAdapter;
import com.orangelabs.rcs.ri.messaging.chat.ChatCursorObserver;
import com.orangelabs.rcs.ri.messaging.chat.ChatView;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * XMS view
 */
public class XmsView extends RcsFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    // @formatter:off
    private static final String[] PROJECTION = new String[] {
        HistoryLog.BASECOLUMN_ID,
        HistoryLog.ID,
        HistoryLog.PROVIDER_ID,
        HistoryLog.MIME_TYPE,
        HistoryLog.CONTENT,
        HistoryLog.TIMESTAMP,
        HistoryLog.STATUS,
        HistoryLog.DIRECTION,
        HistoryLog.CONTACT,
        HistoryLog.EXPIRED_DELIVERY,
        HistoryLog.FILENAME,
        HistoryLog.FILESIZE,
        HistoryLog.TRANSFERRED,
        HistoryLog.REASON_CODE
    };
    // @formatter:on

    private final static int MENU_ITEM_DELETE = 1;

    private final static String EXTRA_CONTACT = "contact";
    /**
     * List of items for contextual menu
     */
    private static final String LOGTAG = LogUtils.getTag(XmsView.class.getSimpleName());
    /**
     * Chat_id is set to contact id for one to one chat and file transfer messages.
     */
    private static final String WHERE_CLAUSE = HistoryLog.CHAT_ID + "=?";
    private final static String ORDER_ASC = HistoryLog.TIMESTAMP + " ASC";

    private final static String[] PROJECTION_UNREAD_MESSAGE = new String[] {
            HistoryLog.PROVIDER_ID, HistoryLog.ID
    };

    private final static String UNREADS_WHERE_CLAUSE = HistoryLog.CHAT_ID + "=? AND "
            + HistoryLog.READ_STATUS + "=" + RcsService.ReadStatus.UNREAD.toInt() + " AND "
            + HistoryLog.DIRECTION + "=" + RcsService.Direction.INCOMING.toInt();

    private static final String OPEN_CONVERSATION = "open_conversation";

    /**
     * The adapter that binds data to the ListView
     */
    protected TalkCursorAdapter mAdapter;

    private Uri mUriHistoryProvider;
    private ContactId mContact;
    private CmsService mCmsService;
    private XmsMessageListener mXmsMessageListener;
    private ChatCursorObserver mObserver;
    private EditText mComposeText;

    /**
     * Forge intent to start XmsView activity
     *
     * @param context The context
     * @param contact The contact ID
     * @return intent
     */
    public static Intent forgeIntentToOpenConversation(Context context, ContactId contact) {
        Intent intent = new Intent(context, XmsView.class);
        intent.setAction(OPEN_CONVERSATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    /**
     * Forge intent to restart XmsView activity on new message event
     *
     * @param ctx The context
     * @param contact The contact ID
     * @param intent intent
     * @return intent
     */
    public static Intent forgeIntentOnNewMessage(Context ctx, ContactId contact, Intent intent) {
        intent.setClass(ctx, XmsView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xms_talk_view);

        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(XmsMessageLog.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        mUriHistoryProvider = uriBuilder.build();

        if (!isServiceConnected(ConnectionManager.RcsServiceName.CMS,
                ConnectionManager.RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }

        mComposeText = (EditText) findViewById(R.id.messageEdit);
        /* Set send button listener */
        Button sendBtn = (Button) findViewById(R.id.SendButton);
        sendBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendText();
            }
        });

        /* Initialize the adapter. */
        mAdapter = new TalkCursorAdapter(this);

        /* Associate the list adapter with the ListView. */
        ListView listView = (ListView) findViewById(R.id.talkListView);
        listView.setAdapter(mAdapter);

        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);

        registerForContextMenu(listView);

        startMonitorServices(ConnectionManager.RcsServiceName.CMS,
                ConnectionManager.RcsServiceName.CONTACT);

        initialize();
        mCmsService = getCmsApi();
        try {
            mCmsService.addEventListener(mXmsMessageListener);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        processIntent(getIntent());
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onCreate");
        }
    }

    private void sendText() {
        final String text = mComposeText.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        try {
            mCmsService.sendTextMessage(mContact, text);
            mComposeText.setText(null);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void initialize() {
        mXmsMessageListener = new XmsMessageListener() {
            @Override
            public void onStateChanged(ContactId contact, String mimeType, String messageId,
                    XmsMessage.State state, XmsMessage.ReasonCode reasonCode) {
                String _reasonCode = RiApplication.sXmsMessageReasonCodes[reasonCode.toInt()];
                String _state = RiApplication.sXmsMessageStates[state.toInt()];
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact + " mime-type=" + mimeType
                            + " msgId=" + messageId + " status=" + _state + " reason="
                            + _reasonCode);
                }
            }

            @Override
            public void onDeleted(ContactId contact, Set<String> messageIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG,
                            "onDeleted contact=" + contact + " for message IDs="
                                    + Arrays.toString(messageIds.toArray()));
                }
            }
        };
    }

    public boolean processIntent(Intent intent) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent ".concat(intent.getAction()));
        }
        ContactId newContact = intent.getParcelableExtra(EXTRA_CONTACT);
        if (newContact == null) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: contact is null");
            }
            return false;
        }
        try {
            if (!newContact.equals(mContact)) {
                /* Either it is the first conversation loading or switch to another conversation */
                loadConversation(newContact);
            }
            /* Set activity title with display name */
            String displayName = RcsContactUtil.getInstance(this).getDisplayName(mContact);
            setTitle(getString(R.string.title_chat, displayName));
            /* Mark as read messages if required */
            Map<String, Integer> msgIdUnreads = getUnreadMessageIds(mContact);
            for (Map.Entry<String, Integer> entryMsgIdUnread : msgIdUnreads.entrySet()) {
                if (XmsMessageLog.HISTORYLOG_MEMBER_ID == entryMsgIdUnread.getValue()) {
                    mCmsService.markMessageAsRead(entryMsgIdUnread.getKey());
                }
            }
            return true;

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return false;
        }
    }

    private void loadConversation(ContactId newContact) throws RcsServiceNotAvailableException,
            RcsGenericException {
        boolean firstLoad = (mContact == null);
        /* Save contact ID */
        mContact = newContact;
        setCursorLoader(firstLoad);
        ChatView.sChatIdOnForeground = mContact.toString();
    }

    /**
     * Get unread messages for contact
     *
     * @param contact contact ID
     * @return Map of unread message IDs associated with the provider ID
     */
    private Map<String, Integer> getUnreadMessageIds(ContactId contact) {
        Map<String, Integer> unReadMessageIDs = new HashMap<>();
        String[] where_args = new String[] {
            contact.toString()
        };
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(mUriHistoryProvider, PROJECTION_UNREAD_MESSAGE,
                    UNREADS_WHERE_CLAUSE, where_args, ORDER_ASC);
            if (!cursor.moveToFirst()) {
                return unReadMessageIDs;
            }
            int msgIdcolumIdx = cursor.getColumnIndexOrThrow(HistoryLog.ID);
            int providerIdColumIdx = cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID);
            do {
                unReadMessageIDs.put(cursor.getString(msgIdcolumIdx),
                        cursor.getInt(providerIdColumIdx));
            } while (cursor.moveToNext());
            return unReadMessageIDs;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void setCursorLoader(boolean firstLoad) {
        if (firstLoad) {
            /*
             * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
             */
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        } else {
            /* We switched from one contact to another: reload history since */
            getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    @Override
    public void onDestroy() {
        if (isServiceConnected(ConnectionManager.RcsServiceName.CMS) && mCmsService != null) {
            try {
                mCmsService.removeEventListener(mXmsMessageListener);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChatView.sChatIdOnForeground = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /* Replace the value of intent */
        setIntent(intent);
        if (isServiceConnected(ConnectionManager.RcsServiceName.CMS,
                ConnectionManager.RcsServiceName.CONTACT)) {
            processIntent(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mContact != null) {
            ChatView.sChatIdOnForeground = mContact.toString();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_xms, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_send_mms:
                startActivity(InitiateMmsTransfer.forgeStartIntent(this, mContact));
                break;
            case R.id.menu_delete_xms:
                try {
                    mCmsService.deleteXmsMessages(mContact);

                } catch (RcsServiceException e) {
                    showException(e);
                }
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        /* Get the list item position */
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        /* Adapt the contextual menu according to the selected item */
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        if (XmsMessageLog.HISTORYLOG_MEMBER_ID == providerId) {
            menu.add(0, MENU_ITEM_DELETE, MENU_ITEM_DELETE, R.string.menu_delete_message);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        String messageId = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected Id=".concat(messageId));
        }
        switch (item.getItemId()) {

            case MENU_ITEM_DELETE:
                try {
                    if (XmsMessageLog.HISTORYLOG_MEMBER_ID == providerId) {
                        mCmsService.deleteXmsMessage(messageId);
                    }
                } catch (RcsServiceException e) {
                    showException(e);
                }
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, mUriHistoryProvider, PROJECTION, WHERE_CLAUSE, new String[] {
            mContact.toString()
        }, ORDER_ASC);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (LOADER_ID == loader.getId()) {
            /*
             * The asynchronous load is complete and the data is now available for use. Only now can
             * we associate the queried Cursor with the CursorAdapter.
             */
            mAdapter.swapCursor(data);
            /**
             * Registering content observer for XMS message content URI. When this content URI will
             * change, this will notify the loader to reload its data.
             */
            if (mObserver != null && !mObserver.getLoader().equals(loader)) {
                ContentResolver resolver = getContentResolver();
                resolver.unregisterContentObserver(mObserver);
                resolver.unregisterContentObserver(mObserver);
                mObserver = null;
            }
            if (mObserver == null) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onLoadFinished: register content observer");
                }
                mObserver = new ChatCursorObserver(new Handler(), loader);
                ContentResolver resolver = getContentResolver();
                resolver.registerContentObserver(XmsMessageLog.CONTENT_URI, true, mObserver);
                resolver.registerContentObserver(ChatLog.Message.CONTENT_URI, true, mObserver);
                resolver.registerContentObserver(FileTransferLog.CONTENT_URI, true, mObserver);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * For whatever reason, the Loader's data is now unavailable. Remove any references to the
         * old data by replacing it with a null Cursor.
         */
        mAdapter.swapCursor(null);
    }
}
