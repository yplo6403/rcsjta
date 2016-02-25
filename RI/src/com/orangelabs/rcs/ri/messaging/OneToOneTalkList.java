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

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.adapter.OneToOneTalkArrayAdapter;
import com.orangelabs.rcs.ri.messaging.adapter.OneToOneTalkArrayItem;
import com.orangelabs.rcs.ri.settings.RiSettings;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * List of one to one conversations from the content provider: XMS + RCS chat + RCS file transfer
 *
 * @author Philippe LEMORDANT
 */
public class OneToOneTalkList extends RcsActivity {

    private CmsService mCmsService;
    private boolean mXmsMessageListenerSet;
    private OneToOneTalkArrayAdapter mAdapter;
    private Handler mHandler = new Handler();
    private List<OneToOneTalkArrayItem> mMessageLogs;
    private static final String LOGTAG = LogUtils.getTag(OneToOneTalkList.class.getSimpleName());

    private XmsMessageListener mXmsMessageListener;
    private ChatService mChatService;
    private FileTransferService mFileTransferService;
    private boolean mOneToOneChatListenerSet;
    private OneToOneChatListener mOneToOneChatListener;
    private Context mCtx;
    private boolean mFileTransferListenerSet;
    private OneToOneFileTransferListener mOneToOneFileTransferListener;
    private OneToOneTalkListUpdate.TaskCompleted mUpdateTalkListListener;
    private static boolean sActivityVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.xms_list);
        initialize();
        if (RiSettings.isSyncAutomatic(this)) {
            try {
                /* Perform CMS synchronization */
                mCmsService.syncAll();

            } catch (RcsServiceNotAvailableException e) {
                Log.w(LOGTAG, "Cannot sync: service is not available!");

            } catch (RcsGenericException e) {
                showExceptionThenExit(e);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /* Replace the value of intent */
        setIntent(intent);
        // TODO test action
        OneToOneTalkListUpdate updateTalkList = new OneToOneTalkListUpdate(this,
                mUpdateTalkListListener);
        updateTalkList.execute();
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
        sActivityVisible = true;
        OneToOneTalkListUpdate updateTalkList = new OneToOneTalkListUpdate(this,
                mUpdateTalkListListener);
        updateTalkList.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sActivityVisible = false;
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
        if (RiSettings.isSyncAutomatic(this)) {
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
        if (RiSettings.isSyncAutomatic(this)) {
            menu.findItem(R.id.menu_sync_xms).setVisible(false);
        }
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

        mUpdateTalkListListener = new OneToOneTalkListUpdate.TaskCompleted() {
            @Override
            public void onTaskComplete(Collection<OneToOneTalkArrayItem> result) {
                if (!sActivityVisible) {
                    return;
                }
                mMessageLogs.clear();
                mMessageLogs.addAll(result);
                /* Sort by descending timestamp */
                Collections.sort(mMessageLogs);
                mAdapter.notifyDataSetChanged();
            }
        };
    }

    /**
     * Notify new conversation event
     */
    public static void notifyNewConversationEvent(Context ctx) {
        if (sActivityVisible) {
            Intent intent = new Intent(ctx, OneToOneTalkList.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        }

    }
}
