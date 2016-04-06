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

package com.orangelabs.rcs.ri.messaging.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsFragmentActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.geoloc.EditGeoloc;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Chat view
 *
 * @author Philippe LEMORDANT
 */
public abstract class ChatView extends RcsFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, IChatView {
    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    protected static final int LOADER_ID = 1;

    private final static int SELECT_GEOLOCATION = 0;

    private static final String LOGTAG = LogUtils.getTag(ChatView.class.getSimpleName());

    /**
     * The adapter that binds data to the ListView
     */
    protected ChatCursorAdapter mAdapter;

    /**
     * Message composer
     */
    protected EditText mComposeText;

    /**
     * Utility class to manage the is-composing status
     */
    protected IsComposingManager mComposingManager;

    /**
     * UI handler
     */
    protected Handler mHandler = new Handler();

    protected ChatService mChatService;

    protected CmsService mCmsService;

    protected Uri mUriHistoryProvider;

    protected FileTransferService mFileTransferService;

    private ChatCursorObserver mObserver;

    public static String sChatIdOnForeground;

    /**
     * Chat message projection
     */
    // @formatter:off
    protected static final String[] PROJ_CHAT_MSG = new String[] {
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

    private final static String[] PROJECTION_UNREAD_MESSAGE = new String[] {
            HistoryLog.PROVIDER_ID, HistoryLog.ID
    };

    private final static String UNREADS_WHERE_CLAUSE = HistoryLog.CHAT_ID + "=? AND "
            + HistoryLog.READ_STATUS + "=" + RcsService.ReadStatus.UNREAD.toInt() + " AND "
            + HistoryLog.DIRECTION + "=" + RcsService.Direction.INCOMING.toInt();

    private final static String ORDER_ASC = HistoryLog.TIMESTAMP + " ASC";

    /**
     * Query sort order
     */
    protected final static String ORDER_CHAT_MSG = HistoryLog.TIMESTAMP + " ASC";

    /**
     * Get unread messages for contact
     *
     * @param ctx the context
     * @param chatId the chat ID
     * @return Map of unread message IDs associated with the provider ID
     */
    public static Map<String, Integer> getUnreadMessageIds(Context ctx, Uri uri, String chatId) {
        Map<String, Integer> unReadMessageIDs = new HashMap<>();
        String[] where_args = new String[] {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(uri, PROJECTION_UNREAD_MESSAGE,
                    UNREADS_WHERE_CLAUSE, where_args, ORDER_ASC);
            if (cursor == null) {
                throw new IllegalStateException("Cannot query unread messages for chatId=" + chatId);
            }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_view);

        initialize();

        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        mUriHistoryProvider = uriBuilder.build();

        /* Set message composer callbacks */
        mComposeText = (EditText) findViewById(R.id.userText);
        mComposeText.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.ACTION_DOWN != event.getAction()) {
                    return false;

                }
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        sendText();
                        return true;
                }
                return false;
            }
        });

        mComposeText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check if the text is not null.
                // we do not wish to consider putting the edit text back to null
                // (like when sending message), is having activity
                if (!TextUtils.isEmpty(s)) {
                    // Warn the composing manager that we have some activity
                    if (mComposingManager != null) {
                        mComposingManager.hasActivity();
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        /* Set send button listener */
        Button sendBtn = (Button) findViewById(R.id.send_button);
        sendBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendText();
            }
        });

        /* Initialize the adapter. */
        mAdapter = new ChatCursorAdapter(this, isSingleChat());

        // Associate the list adapter with the ListView.
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(mAdapter);
        registerForContextMenu(listView);

        if (!isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACT,
                RcsServiceName.CAPABILITY, RcsServiceName.FILE_TRANSFER, RcsServiceName.CMS)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CHAT, RcsServiceName.CONTACT,
                RcsServiceName.CAPABILITY, RcsServiceName.FILE_TRANSFER, RcsServiceName.CMS);
        mChatService = getChatApi();
        mCmsService = getCmsApi();
        mFileTransferService = getFileTransferApi();
        processIntent(getIntent());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceConnected(RcsServiceName.CHAT) && mChatService != null) {
            try {
                removeChatEventListener(mChatService);
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Replace the value of intent
        setIntent(intent);
        if (isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACT,
                RcsServiceName.FILE_TRANSFER)) {
            processIntent(intent);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (LOADER_ID != loader.getId()) {
            return;
        }

        /*
         * The asynchronous load is complete and the data is now available for use. Only now can we
         * associate the queried Cursor with the CursorAdapter.
         */
        mAdapter.swapCursor(cursor);
        /**
         * Registering content observer for chat message and file transfer content URIs. When these
         * content URIs will change, this will notify the loader to reload its data.
         */
        if (mObserver != null && !mObserver.getLoader().equals(loader)) {
            ContentResolver resolver = getContentResolver();
            resolver.unregisterContentObserver(mObserver);
            mObserver = null;
        }
        if (mObserver == null) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onLoadFinished: register content observer");
            }
            mObserver = new ChatCursorObserver(new Handler(), loader);
            ContentResolver resolver = getContentResolver();
            resolver.registerContentObserver(ChatLog.Message.CONTENT_URI, true, mObserver);
            resolver.registerContentObserver(FileTransferLog.CONTENT_URI, true, mObserver);
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

    private void sendText() {
        String text = mComposeText.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        try {
            sendMessage(text);
            /* Warn the composing manager that the message was sent */
            mComposingManager.messageWasSent();
            mComposeText.setText(null);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }

    }

    private void sendGeoloc(Geoloc geoloc) {
        try {
            sendMessage(geoloc);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    /**
     * Add quick text
     */
    protected void addQuickText() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_select_quicktext);
        builder.setCancelable(true);
        builder.setItems(R.array.select_quicktext, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String[] items = getResources().getStringArray(R.array.select_quicktext);
                mComposeText.append(items[which]);
            }
        });
        registerDialog(builder.show());
    }

    /**
     * Get a geoloc
     */
    protected void getGeoLoc() {
        // Start a new activity to send a geolocation
        startActivityForResult(new Intent(this, EditGeoloc.class), SELECT_GEOLOCATION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case SELECT_GEOLOCATION:
                Geoloc geoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC);
                sendGeoloc(geoloc);
                break;
        }
    }

    /**
     * Display composing event for contact
     * 
     * @param contact the contact ID
     * @param status True if contact is composing
     */
    protected void displayComposingEvent(final ContactId contact, final boolean status) {
        final String from = RcsContactUtil.getInstance(this).getDisplayName(contact);
        // Execute on UI handler since callback is executed from service
        mHandler.post(new Runnable() {
            public void run() {
                TextView view = (TextView) findViewById(R.id.isComposingText);
                if (status) {
                    // Display is-composing notification
                    view.setText(getString(R.string.label_contact_is_composing, from));
                    view.setVisibility(View.VISIBLE);
                } else {
                    // Hide is-composing notification
                    view.setVisibility(View.GONE);
                }
            }
        });
    }

    protected void setCursorLoader(boolean firstLoad) {
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
}
