/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010 France Telecom S.A.
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
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.cms.CmsSynchronizationListener;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageListener;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.adapter.OneToOneTalkArrayAdapter;
import com.orangelabs.rcs.ri.messaging.adapter.OneToOneTalkArrayItem;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * List of one to one conversations from the content provider: XMS + RCS chat + RCS file transfer
 *
 * @author Philippe LEMORDANT
 */
public class OneToOneTalkList extends RcsActivity {
    // @formatter:off
    private static final String[] PROJECTION = new String[]{
            HistoryLog.BASECOLUMN_ID,
            HistoryLog.ID,
            HistoryLog.PROVIDER_ID,
            HistoryLog.MIME_TYPE,
            HistoryLog.CONTENT,
            HistoryLog.TIMESTAMP,
            HistoryLog.STATUS,
            HistoryLog.DIRECTION,
            HistoryLog.CONTACT,
            HistoryLog.FILENAME,
            HistoryLog.FILESIZE,
            HistoryLog.TRANSFERRED,
            HistoryLog.READ_STATUS,
            HistoryLog.REASON_CODE
    };
    // @formatter:on

    private static final String WHERE_CLAUSE = HistoryLog.CHAT_ID + "=" + HistoryLog.CONTACT;

    private CmsService mCmsService;
    private boolean mXmsMessageListenerSet;
    private Uri mUriHistoryProvider;
    private OneToOneTalkArrayAdapter mAdapter;
    private Handler mHandler = new Handler();
    private List<OneToOneTalkArrayItem> mMessageLogs;
    private static final String LOGTAG = LogUtils.getTag(OneToOneTalkList.class.getSimpleName());

    private CmsSynchronizationListener mCmsSynchronizationListener;
    private boolean mCmsSynchronizationListenerSet;
    private XmsMessageListener mXmsMessageListener;
    private ChatService mChatService;
    private FileTransferService mFileTransferService;
    private boolean mOneToOneChatListenerSet;
    private OneToOneChatListener mOneToOneChatListener;
    private Context mCtx;
    private boolean mFileTransferListenerSet;
    private OneToOneFileTransferListener mOneToOneFileTransferListener;
    private MessagingObserver mObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.xms_list);
        initialize();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCmsService == null) {
            return;
        }
        try {
            if (mXmsMessageListenerSet) {
                mCmsService.removeEventListener(mXmsMessageListener);
            }
            if (mCmsSynchronizationListenerSet) {
                mCmsService.removeEventListener(mCmsSynchronizationListener);
            }
            if (mOneToOneChatListenerSet) {
                mChatService.removeEventListener(mOneToOneChatListener);
            }
            if (mFileTransferListenerSet) {
                mFileTransferService.removeEventListener(mOneToOneFileTransferListener);
            }
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mObserver == null) {
            queryHistoryLogAndRefreshView();
            mObserver = new MessagingObserver(mHandler);
            ContentResolver resolver = getContentResolver();
            resolver.registerContentObserver(XmsMessageLog.CONTENT_URI, true, mObserver);
            resolver.registerContentObserver(ChatLog.Message.CONTENT_URI, true, mObserver);
            resolver.registerContentObserver(FileTransferLog.CONTENT_URI, true, mObserver);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mObserver != null) {
            getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_log_xms, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isServiceConnected(RcsServiceName.CMS)) {
            menu.findItem(R.id.menu_clear_log).setVisible(false);
            menu.findItem(R.id.menu_sync_xms).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.menu_clear_log:
                    /* Delete all XMS messages */
                    if (!isServiceConnected(RcsServiceName.CMS, RcsServiceName.CHAT,
                            RcsServiceName.FILE_TRANSFER)) {
                        showMessage(R.string.label_service_not_available);
                        break;
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "delete one to one conversations");
                    }
                    if (!mXmsMessageListenerSet) {
                        mCmsService.addEventListener(mXmsMessageListener);
                        mXmsMessageListenerSet = true;
                    }
                    mCmsService.deleteXmsMessages();
                    if (!mOneToOneChatListenerSet) {
                        mChatService.addEventListener(mOneToOneChatListener);
                        mOneToOneChatListenerSet = true;
                    }
                    mChatService.deleteOneToOneChats();
                    if (!mFileTransferListenerSet) {
                        mFileTransferService.addEventListener(mOneToOneFileTransferListener);
                        mFileTransferListenerSet = true;
                    }
                    mFileTransferService.deleteOneToOneFileTransfers();
                    break;

                case R.id.menu_sync_xms:
                    /* Start a sync with CMS */
                    if (!isServiceConnected(RcsServiceName.CMS)) {
                        showMessage(R.string.label_service_not_available);
                        break;
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "start a XMS sync");
                    }
                    if (!mCmsSynchronizationListenerSet) {
                        mCmsService.addEventListener(mCmsSynchronizationListener);
                        mCmsSynchronizationListenerSet = true;
                    }
                    mCmsService.syncAll();
                    break;
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_log_xms_item, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /* Get selected item */
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        OneToOneTalkArrayItem message = mAdapter.getItem(info.position);
        ContactId contact = message.getContact();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected contact=".concat(contact.toString()));
        }
        try {
            switch (item.getItemId()) {
                case R.id.menu_open_talk:
                    startActivity(OneToOneTalkView.forgeIntentToOpenConversation(this, contact));
                    return true;

                case R.id.menu_delete_message:
                    if (!isServiceConnected(RcsServiceName.CMS, RcsServiceName.CHAT,
                            RcsServiceName.FILE_TRANSFER)) {
                        showMessage(R.string.error_conversation_delete);
                        return true;
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Delete conversation for contact=".concat(contact.toString()));
                    }
                    if (!mXmsMessageListenerSet) {
                        mCmsService.addEventListener(mXmsMessageListener);
                        mXmsMessageListenerSet = true;
                    }
                    mCmsService.deleteXmsMessages(contact);
                    if (!mOneToOneChatListenerSet) {
                        mChatService.addEventListener(mOneToOneChatListener);
                        mOneToOneChatListenerSet = true;
                    }
                    mChatService.deleteOneToOneChat(contact);
                    if (!mFileTransferListenerSet) {
                        mFileTransferService.addEventListener(mOneToOneFileTransferListener);
                        mFileTransferListenerSet = true;
                    }
                    mFileTransferService.deleteOneToOneFileTransfers(contact);
                    return true;

                case R.id.menu_sync_xms:
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Sync XMS messages for contact=".concat(contact.toString()));
                    }
                    if (!mCmsSynchronizationListenerSet) {
                        mCmsService.addEventListener(mCmsSynchronizationListener);
                        mCmsSynchronizationListenerSet = true;
                    }
                    mCmsService.syncOneToOneConversation(contact);
                    return true;

                default:
                    return super.onContextItemSelected(item);
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return true;
        }
    }

    private void initialize() {
        mCtx = this;
        mMessageLogs = new ArrayList<>();

        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(XmsMessageLog.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        mUriHistoryProvider = uriBuilder.build();

        mCmsService = getCmsApi();
        mChatService = getChatApi();
        mFileTransferService = getFileTransferApi();

        ListView listView = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);
        registerForContextMenu(listView);

        mAdapter = new OneToOneTalkArrayAdapter(this, mMessageLogs);
        listView.setAdapter(mAdapter);

        mOneToOneFileTransferListener = new OneToOneFileTransferListener() {
            @Override
            public void onStateChanged(ContactId contact, String transferId,
                    FileTransfer.State state, FileTransfer.ReasonCode reasonCode) {
            }

            @Override
            public void onProgressUpdate(ContactId contact, String transferId, long currentSize,
                    long totalSize) {
            }

            @Override
            public void onDeleted(final ContactId contact, Set<String> transferIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onDeleted contact=" + contact + " FT IDs=" + transferIds);
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        Utils.displayLongToast(
                                mCtx,
                                getString(R.string.label_delete_file_transfer_success,
                                        contact.toString()));
                    }
                });
            }
        };
        mOneToOneChatListener = new OneToOneChatListener() {
            @Override
            public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
                    ChatLog.Message.Content.Status status,
                    ChatLog.Message.Content.ReasonCode reasonCode) {
            }

            @Override
            public void onComposingEvent(ContactId contact, boolean status) {
            }

            @Override
            public void onMessagesDeleted(final ContactId contact, Set<String> msgIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessagesDeleted contact=" + contact + " chat IDs=" + msgIds);
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        Utils.displayLongToast(mCtx,
                                getString(R.string.label_delete_chat_success, contact.toString()));
                    }
                });
            }
        };
        mXmsMessageListener = new XmsMessageListener() {

            @Override
            public void onDeleted(final ContactId contact, Set<String> msgIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onDeleted contact=" + contact + " XMS IDs=" + msgIds);
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        Utils.displayLongToast(OneToOneTalkList.this,
                                getString(R.string.label_xms_delete_success, contact.toString()));
                    }
                });
            }

            @Override
            public void onStateChanged(ContactId contact, String mimeType, String messageId,
                    XmsMessage.State state, XmsMessage.ReasonCode reasonCode) {
            }
        };

        mCmsSynchronizationListener = new CmsSynchronizationListener() {
            @Override
            public void onAllSynchronized() {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onAllSynchronized");
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        Utils.displayLongToast(OneToOneTalkList.this,
                                getString(R.string.label_cms_sync_end));
                    }
                });
            }

            @Override
            public void onOneToOneConversationSynchronized(final ContactId contact) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onOneToOneConversationSynchronized contact=" + contact);
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        Utils.displayLongToast(OneToOneTalkList.this,
                                getString(R.string.label_cms_sync_talk_end, contact.toString()));
                    }
                });
            }

            @Override
            public void onGroupConversationSynchronized(String chatId) {
            }
        };
    }

    void queryHistoryLogAndRefreshView() {
        Map<ContactId, OneToOneTalkArrayItem> dataMap = new HashMap<>();
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(mUriHistoryProvider, PROJECTION, WHERE_CLAUSE,
                    null, null);
            if (cursor == null) {
                throw new IllegalStateException("Cannot query History Log");
            }
            int columnTimestamp = cursor.getColumnIndexOrThrow(HistoryLog.TIMESTAMP);
            int columnProviderId = cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID);
            int columnDirection = cursor.getColumnIndexOrThrow(HistoryLog.DIRECTION);
            int columnContact = cursor.getColumnIndexOrThrow(HistoryLog.CONTACT);
            int columnContent = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
            int columnFilename = cursor.getColumnIndexOrThrow(HistoryLog.FILENAME);
            int columnStatus = cursor.getColumnIndexOrThrow(HistoryLog.STATUS);
            int columnReason = cursor.getColumnIndexOrThrow(HistoryLog.REASON_CODE);
            int columnMimeType = cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE);
            int columnFileSize = cursor.getColumnIndexOrThrow(HistoryLog.FILESIZE);
            int columnTransferred = cursor.getColumnIndexOrThrow(HistoryLog.TRANSFERRED);
            while (cursor.moveToNext()) {
                long timestamp = cursor.getLong(columnTimestamp);
                String phoneNumber = cursor.getString(columnContact);
                ContactId contact = ContactUtil.formatContact(phoneNumber);
                OneToOneTalkArrayItem item = dataMap.get(contact);
                if (item != null && timestamp < item.getTimestamp()) {
                    continue;
                }
                int providerId = cursor.getInt(columnProviderId);
                int state = cursor.getInt(columnStatus);
                int reason = cursor.getInt(columnReason);
                RcsService.Direction dir = RcsService.Direction.valueOf(cursor
                        .getInt(columnDirection));
                String content = cursor.getString(columnContent);
                String mimeType = cursor.getString(columnMimeType);
                switch (providerId) {
                    case XmsMessageLog.HISTORYLOG_MEMBER_ID:
                    case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                        dataMap.put(contact, new OneToOneTalkArrayItem(providerId, contact,
                                timestamp, state, reason, dir, content, mimeType));
                        break;

                    case FileTransferLog.HISTORYLOG_MEMBER_ID:
                        String filename = cursor.getString(columnFilename);
                        long fileSize = cursor.getLong(columnFileSize);
                        long transferred = cursor.getLong(columnTransferred);
                        dataMap.put(contact, new OneToOneTalkArrayItem(contact, timestamp, state,
                                reason, dir, content, mimeType, filename, fileSize, transferred));
                        break;

                    default:
                        throw new IllegalArgumentException("Invalid provider ID: '" + providerId
                                + "'!");
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        mMessageLogs.clear();
        mMessageLogs.addAll(dataMap.values());
        /* Sort by ascending timestamp */
        Collections.sort(mMessageLogs);
        mAdapter.notifyDataSetChanged();
    }

    private class MessagingObserver extends ContentObserver {
        public MessagingObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            queryHistoryLogAndRefreshView();
        }
    }
}
