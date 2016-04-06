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

import static com.orangelabs.rcs.ri.utils.FileUtils.takePersistableContentUriPermission;

import com.gsma.services.rcs.CommonServiceConfiguration;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.OneToOneChat;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.chat.OneToOneChatListener;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.cms.CmsSynchronizationListener;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageIntent;
import com.gsma.services.rcs.cms.XmsMessageListener;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsFragmentActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.cms.messaging.InitiateMmsTransfer;
import com.orangelabs.rcs.ri.cms.messaging.SendMmsInBackground;
import com.orangelabs.rcs.ri.messaging.adapter.OneToOneTalkCursorAdapter;
import com.orangelabs.rcs.ri.messaging.chat.ChatCursorObserver;
import com.orangelabs.rcs.ri.messaging.chat.ChatPendingIntentManager;
import com.orangelabs.rcs.ri.messaging.chat.ChatView;
import com.orangelabs.rcs.ri.messaging.chat.IsComposingManager;
import com.orangelabs.rcs.ri.messaging.chat.single.SendSingleFile;
import com.orangelabs.rcs.ri.messaging.chat.single.SingleChatIntentService;
import com.orangelabs.rcs.ri.messaging.filetransfer.FileTransferIntentService;
import com.orangelabs.rcs.ri.messaging.geoloc.EditGeoloc;
import com.orangelabs.rcs.ri.settings.RiSettings;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Telephony;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One to one talk view : aggregates the RCS IM and XMS messages.
 *
 * @author Philippe LEMORDANT
 */
public class OneToOneTalkView extends RcsFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

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
            HistoryLog.EXPIRED_DELIVERY,
            HistoryLog.FILENAME,
            HistoryLog.FILESIZE,
            HistoryLog.TRANSFERRED,
            HistoryLog.REASON_CODE };
    // @formatter:on

    private final static String EXTRA_CONTACT = "contact";

    private static final String LOGTAG = LogUtils.getTag(OneToOneTalkView.class.getSimpleName());
    /**
     * Chat_id is set to contact id for one to one chat and file transfer messages.
     */
    private static final String WHERE_CLAUSE = HistoryLog.CHAT_ID + "=?";
    private final static String ORDER_ASC = HistoryLog.TIMESTAMP + " ASC";

    private static final String OPEN_TALK = "open_talk";

    private final static int SELECT_GEOLOCATION = 0;

    /**
     * The adapter that binds data to the ListView
     */
    private OneToOneTalkCursorAdapter mAdapter;
    private Uri mUriHistoryProvider;
    private ContactId mContact;
    private CmsService mCmsService;
    private CmsSynchronizationListener mCmsSynchronizationListener;
    private boolean mCmsSynchronizationListenerSet;
    private XmsMessageListener mXmsMessageListener;
    private boolean mXmsMessageListenerSet;
    private ChatCursorObserver mObserver;
    private EditText mComposeText;
    private ChatService mChatService;
    private OneToOneChat mChat;
    private FileTransferService mFileTransferService;
    private OneToOneChatListener mChatListener;
    private Handler mHandler;
    private AlertDialog mClearUndeliveredAlertDialog;
    private DialogInterface.OnClickListener mResendChat;
    private DialogInterface.OnClickListener mDoNotResendChat;
    private DialogInterface.OnCancelListener mUndeliveredCancelListener;
    private DialogInterface.OnClickListener mResendFt;
    private DialogInterface.OnClickListener mDoNotResendFt;
    /**
     * Utility class to manage the is-composing status
     */
    private IsComposingManager mComposingManager;
    private CapabilityService mCapabilityService;
    private CapabilitiesListener mCapabilitiesListener;
    private boolean mRcsMode;
    private Button mSendBtn;
    private boolean mSaveDoNotAskAgainResend;
    private RiSettings.PreferenceResendRcs mPreferenceResendChat;
    private RiSettings.PreferenceResendRcs mPreferenceResendFt;
    private boolean mCanSendMms;
    private Context mCtx;

    private static boolean sActivityVisible;

    // @formatter:off
    private static final Set<String> sAllowedIntentActions = new HashSet<>(Arrays.asList(
            OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED,
            OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE,
            FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED,
            XmsMessageIntent.ACTION_NEW_XMS_MESSAGE,
            OPEN_TALK ));
    // @formatter:on

    /**
     * Forge intent to start XmsView activity
     *
     * @param context The context
     * @param contact The contact ID
     * @return intent
     */
    public static Intent forgeIntentToOpenConversation(Context context, ContactId contact) {
        Intent intent = new Intent(context, OneToOneTalkView.class);
        intent.setAction(OPEN_TALK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    /**
     * Forge intent to start OneToOneTalkView activity upon reception of a stack event
     *
     * @param ctx The context
     * @param contact The contact ID
     * @param intent intent
     * @return intent
     */
    public static Intent forgeIntentOnStackEvent(Context ctx, ContactId contact, Intent intent) {
        intent.setClass(ctx, OneToOneTalkView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_view);
        startMonitorServices(ConnectionManager.RcsServiceName.CMS,
                ConnectionManager.RcsServiceName.CONTACT, ConnectionManager.RcsServiceName.CHAT,
                ConnectionManager.RcsServiceName.FILE_TRANSFER,
                ConnectionManager.RcsServiceName.CAPABILITY);
        try {
            initialize();
            mChatService.addEventListener(mChatListener);
            mCapabilityService.addCapabilitiesListener(mCapabilitiesListener);
            addCmsServiceListeners();

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return;
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
            if (mRcsMode && mChat != null) {
                mChat.sendMessage(text);
            } else {
                mCmsService.sendTextMessage(mContact, text);
            }
            mComposeText.setText(null);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void initialize() throws RcsGenericException, RcsServiceNotAvailableException {
        mCtx = this;
        /* Set send button listener */
        mSendBtn = (Button) findViewById(R.id.send_button);
        mSendBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendText();
            }
        });

        mHandler = new Handler();

        mPreferenceResendChat = RiSettings.getPreferenceResendChat(this);
        mPreferenceResendFt = RiSettings.getPreferenceResendFileTransfer(this);

        mResendChat = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    if (mSaveDoNotAskAgainResend) {
                        mPreferenceResendChat = RiSettings.PreferenceResendRcs.SEND_TO_XMS;
                        RiSettings.savePreferenceResendChat(mCtx, mPreferenceResendChat);
                    }
                    Set<String> msgIds = SingleChatIntentService.getUndelivered(mCtx, mContact);
                    if (msgIds.isEmpty()) {
                        return;
                    }
                    for (String msgId : msgIds) {
                        resendChatMessageViaSms(msgId);
                    }
                } catch (RcsServiceException e) {
                    showException(e);
                } finally {
                    mClearUndeliveredAlertDialog = null;
                }
            }
        };
        mDoNotResendChat = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mSaveDoNotAskAgainResend) {
                    mPreferenceResendChat = RiSettings.PreferenceResendRcs.DO_NOTHING;
                    RiSettings.savePreferenceResendChat(mCtx, mPreferenceResendChat);
                }
                Set<String> msgIds = SingleChatIntentService.getUndelivered(mCtx, mContact);
                if (msgIds.isEmpty()) {
                    return;
                }
                try {
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Clear delivery expiration for IDs=" + msgIds);
                    }
                    mChatService.clearMessageDeliveryExpiration(msgIds);

                } catch (RcsServiceException e) {
                    showException(e);
                } finally {
                    mClearUndeliveredAlertDialog = null;
                }
            }
        };
        mUndeliveredCancelListener = new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mClearUndeliveredAlertDialog = null;
            }
        };
        mResendFt = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    if (mSaveDoNotAskAgainResend) {
                        mPreferenceResendFt = RiSettings.PreferenceResendRcs.SEND_TO_XMS;
                        RiSettings.savePreferenceResendFileTransfer(mCtx, mPreferenceResendFt);
                    }
                    Set<String> transferIds = FileTransferIntentService.getUndelivered(mCtx,
                            mContact);
                    if (transferIds.isEmpty()) {
                        return;
                    }
                    for (String id : transferIds) {
                        resendFileTransferViaMms(id);
                    }
                } catch (RcsServiceException e) {
                    showException(e);
                } finally {
                    mClearUndeliveredAlertDialog = null;
                }
            }
        };
        mDoNotResendFt = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    if (mSaveDoNotAskAgainResend) {
                        mPreferenceResendFt = RiSettings.PreferenceResendRcs.DO_NOTHING;
                        RiSettings.savePreferenceResendFileTransfer(mCtx, mPreferenceResendFt);
                    }
                    Set<String> transferIds = FileTransferIntentService.getUndelivered(mCtx,
                            mContact);
                    if (transferIds.isEmpty()) {
                        return;
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Clear delivery expiration for IDs=" + transferIds);
                    }
                    mFileTransferService.clearFileTransferDeliveryExpiration(transferIds);

                } catch (RcsServiceException e) {
                    showException(e);
                } finally {
                    mClearUndeliveredAlertDialog = null;
                }
            }
        };

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
                    Log.d(LOGTAG, "onDeleted contact=" + contact + " for msg IDs=" + messageIds);
                }
            }
        };

        mCmsSynchronizationListener = new CmsSynchronizationListener() {
            @Override
            public void onAllSynchronized() {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onAllSynchronized");
                }
                if (!sActivityVisible) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onOneToOneConversationSynchronized(final ContactId contact) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onOneToOneConversationSynchronized contact=" + contact);
                }
                if (!sActivityVisible) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onGroupConversationSynchronized(final String chatId) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onGroupConversationSynchronized chatId=" + chatId);
                }
                if (!sActivityVisible) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        };

        mChatListener = new OneToOneChatListener() {

            /* Callback called when an Is-composing event has been received */
            @Override
            public void onComposingEvent(ContactId contact, boolean status) {
                /* Discard event if not for current contact */
                if (!mContact.equals(contact)) {
                    return;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onComposingEvent contact=" + contact + " status=" + status);
                }
                displayComposingEvent(contact, status);
            }

            @Override
            public void onMessageStatusChanged(ContactId contact, String mimeType, String msgId,
                    ChatLog.Message.Content.Status status,
                    ChatLog.Message.Content.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessageStatusChanged contact=" + contact + " mime-type="
                            + mimeType + " msgId=" + msgId + " status=" + status);
                }
            }

            @Override
            public void onMessagesDeleted(ContactId contact, Set<String> msgIds) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onMessagesDeleted contact=" + contact + " for IDs=" + msgIds);
                }
            }

        };
        mCapabilitiesListener = new CapabilitiesListener() {
            @Override
            public void onCapabilitiesReceived(ContactId contact, Capabilities capabilities) {
                if (mContact == null || !mContact.equals(contact)) {
                    /* Discard new capabilities not for current contact */
                    return;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onCapabilitiesReceived contact=" + contact + " IM="
                            + capabilities.isImSessionSupported());
                }
                try {
                    /*
                     * We received new capabilities for current contact: re-evaluate the composer
                     * capabilities.
                     */
                    final boolean allowedToSendRcsMessage = mChat.isAllowedToSendMessage();
                    // Execute on UI handler since callback is executed from service
                    mHandler.post(new Runnable() {
                        public void run() {
                            setRcsMode(allowedToSendRcsMessage);
                        }
                    });

                } catch (RcsServiceException e) {
                    Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
                }

            }
        };
        mCmsService = getCmsApi();
        mCanSendMms = mCmsService.isAllowedToSendMultimediaMessage();
        mChatService = getChatApi();
        mCapabilityService = getCapabilityApi();
        mFileTransferService = getFileTransferApi();

        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        uriBuilder.appendProvider(XmsMessageLog.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        uriBuilder.appendProvider(FileTransferLog.HISTORYLOG_MEMBER_ID);
        mUriHistoryProvider = uriBuilder.build();

        mComposeText = (EditText) findViewById(R.id.userText);
        ChatServiceConfiguration configuration = mChatService.getConfiguration();
        // Set max label length
        int maxMsgLength = configuration.getOneToOneChatMessageMaxLength();
        if (maxMsgLength > 0) {
            /* Set the message composer max length */
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
            mComposeText.setFilters(filterArray);
        }
        IsComposingManager.INotifyComposing iNotifyComposing = new IsComposingManager.INotifyComposing() {
            public void setTypingStatus(boolean isTyping) {
                try {
                    if (mChat == null || !mRcsMode) {
                        return;
                    }
                    mChat.setComposingStatus(isTyping);
                    if (LogUtils.isActive) {
                        Boolean _isTyping = isTyping;
                        Log.d(LOGTAG, "sendIsComposingEvent ".concat(_isTyping.toString()));
                    }
                } catch (RcsGenericException e) {
                    showException(e);
                }
            }
        };
        /* Instantiate the composing manager */
        mComposingManager = new IsComposingManager(configuration.getIsComposingTimeout(),
                iNotifyComposing);
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

        /* Initialize the adapter. */
        mAdapter = new OneToOneTalkCursorAdapter(this);

        /* Associate the list adapter with the ListView. */
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setDivider(null);
        listView.setAdapter(mAdapter);

        registerForContextMenu(listView);
    }

    private void markMessagesAsRead() throws RcsGenericException, RcsPersistentStorageException,
            RcsServiceNotAvailableException {
        /* Mark as read messages if required */
        Map<String, Integer> msgIdUnReads = ChatView.getUnreadMessageIds(this, mUriHistoryProvider,
                mContact.toString());
        for (Map.Entry<String, Integer> entryMsgIdUnread : msgIdUnReads.entrySet()) {
            int providerId = entryMsgIdUnread.getValue();
            String id = entryMsgIdUnread.getKey();
            switch (providerId) {
                case XmsMessageLog.HISTORYLOG_MEMBER_ID:
                    mCmsService.markMessageAsRead(id);
                    break;

                case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                    mChatService.markMessageAsRead(id);
                    break;

                case FileTransferLog.HISTORYLOG_MEMBER_ID:
                    mFileTransferService.markFileTransferAsRead(id);
                    break;

                default:
                    throw new IllegalArgumentException("Invalid provider ID=" + providerId);
            }
        }
    }

    private boolean processIntent(Intent intent) {
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
        String action = intent.getAction();
        if (action == null) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: action is null");
            }
            return false;
        }
        if (!sAllowedIntentActions.contains(action)) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: unauthorized action " + action);
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
            setTitle(displayName);
            markMessagesAsRead();
            switch (action) {
                case OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE:
                    /*
                     * Open chat to accept session if the parameter IM SESSION START is 0. Client
                     * application is not aware of the one to one chat session state nor of the IM
                     * session start mode so we call the method systematically.
                     */
                    mChat.openChat();
                    break;

                case OneToOneChatIntent.ACTION_MESSAGE_DELIVERY_EXPIRED:
                    processUndeliveredMessages(displayName);
                    break;

                case FileTransferIntent.ACTION_FILE_TRANSFER_DELIVERY_EXPIRED:
                    processUndeliveredFileTransfers(displayName, mCanSendMms);
                    break;

                case XmsMessageIntent.ACTION_NEW_XMS_MESSAGE:
                    /* If we receive a XMS, it is a criteria to switch to XMS */
                    setRcsMode(false);
                    break;
            }
            return true;

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return false;
        }
    }

    private void loadConversation(ContactId newContact) throws RcsServiceNotAvailableException,
            RcsGenericException, RcsPersistentStorageException {
        boolean firstLoad = (mContact == null);
        /* Save contact ID */
        mContact = newContact;

        ChatPendingIntentManager pendingIntentManager = ChatPendingIntentManager
                .getChatPendingIntentManager(this);
        pendingIntentManager.clearNotification(mContact.toString());

        /*
         * Open chat so that if the parameter IM SESSION START is 0 then the session is accepted
         * now.
         */
        mChat = mChatService.getOneToOneChat(mContact);
        setCursorLoader(firstLoad);
        ChatView.sChatIdOnForeground = mContact.toString();
        setRcsMode(mChat.isAllowedToSendMessage());

        if (RiSettings.isSyncAutomatic(this)) {
            try {
                /* Perform CMS synchronization */
                mCmsService.syncOneToOneConversation(mContact);

            } catch (RcsServiceNotAvailableException e) {
                Log.w(LOGTAG, "Cannot sync: service is not available!");
            }
        }
        if (!mRcsMode) {
            /* Request for capabilities ony if they are not available or expired */
            requestCapabilities(mContact);
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
        try {
            if (isServiceConnected(ConnectionManager.RcsServiceName.CMS) && mCmsService != null) {
                mCmsService.removeEventListener(mXmsMessageListener);
                mCmsService.removeEventListener(mCmsSynchronizationListener);
            }
            if (isServiceConnected(ConnectionManager.RcsServiceName.CHAT) && mChatService != null) {
                mChatService.removeEventListener(mChatListener);
            }
            if (isServiceConnected(ConnectionManager.RcsServiceName.CAPABILITY)
                    && mCapabilityService != null) {
                mCapabilityService.removeCapabilitiesListener(mCapabilitiesListener);
            }
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sActivityVisible = false;
        ChatView.sChatIdOnForeground = null;
        removeCmsServiceListeners();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /* Replace the value of intent */
        setIntent(intent);
        processIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sActivityVisible = true;
        if (mContact != null) {
            ChatView.sChatIdOnForeground = mContact.toString();
        }
        addCmsServiceListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_1to1_talk, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_send_geoloc).setVisible(mRcsMode);
        menu.findItem(R.id.menu_send_rcs_file).setVisible(mRcsMode);
        menu.findItem(R.id.menu_switch_to_rcs).setVisible(!mRcsMode);
        menu.findItem(R.id.menu_switch_to_sms).setVisible(mRcsMode);
        menu.findItem(R.id.menu_sync_cms).setVisible(!RiSettings.isSyncAutomatic(this));
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case SELECT_GEOLOCATION:
                Geoloc geoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC);
                try {
                    if (mRcsMode && mChat != null) {
                        mChat.sendMessage(geoloc);
                    } else {
                        mCmsService.sendTextMessage(mContact, geoloc.toString());
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                break;
        }
    }

    private void selectMimeTypeThenSendMms() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_add_parts));
        final String[] choices;
        choices = getResources().getStringArray(R.array.mms_select_parts);
        builder.setItems(choices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Select to add file of mime type " + choices[which]);
                }
                switch (which) {
                    case 0:
                        startActivity(InitiateMmsTransfer.forgeStartIntent(mCtx, mContact,
                                "image/*"));
                        break;

                    default:
                        showMessage(getString(R.string.err_not_implemented, choices[which]));
                }
            }
        });
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.menu_send_geoloc:
                    /* Start a new activity to select a geolocation */
                    startActivityForResult(new Intent(this, EditGeoloc.class), SELECT_GEOLOCATION);
                    break;

                case R.id.menu_send_rcs_file:
                    SendSingleFile.startActivity(this, mContact);
                    break;

                case R.id.menu_send_mms:
                    if (mCanSendMms) {
                        selectMimeTypeThenSendMms();
                    } else {
                        Uri _uri = Uri.parse("tel:" + mContact);
                        Intent sendIntent = new Intent(Intent.ACTION_VIEW, _uri);
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(this);
                            sendIntent.setPackage(defaultSmsPackageName);
                        }
                        sendIntent.putExtra("address", mContact.toString());
                        sendIntent.putExtra("sms_body", "");
                        sendIntent.setType("vnd.android-dir/mms-sms");
                        startActivity(sendIntent);
                    }
                    break;

                case R.id.menu_delete_talk:
                    mCmsService.deleteXmsMessages(mContact);
                    mFileTransferService.deleteOneToOneFileTransfers(mContact);
                    mChatService.deleteOneToOneChat(mContact);
                    break;

                case R.id.menu_switch_to_rcs:
                    if (mChat.isAllowedToSendMessage()) {
                        setRcsMode(true);
                    } else {
                        requestCapabilities(mContact);
                    }
                    break;

                case R.id.menu_switch_to_sms:
                    setRcsMode(false);
                    break;

                case R.id.menu_sync_cms:
                    try {
                        mCmsService.syncOneToOneConversation(mContact);
                    } catch (RcsServiceNotAvailableException e) {
                        showException(e);
                    }
                    break;
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_1to1_talk_item, menu);
        /* Get the list item position */
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        /* Adapt the contextual menu according to the selected item */
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        RcsService.Direction direction = RcsService.Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(HistoryLog.DIRECTION)));
        if (FileTransferLog.HISTORYLOG_MEMBER_ID == providerId) {
            String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
            if (Utils.isImageType(mimeType)) {
                if (RcsService.Direction.INCOMING == direction) {
                    Long transferred = cursor.getLong(cursor
                            .getColumnIndexOrThrow(HistoryLog.TRANSFERRED));
                    Long size = cursor.getLong(cursor.getColumnIndexOrThrow(HistoryLog.FILESIZE));
                    if (!size.equals(transferred)) {
                        /* file is not transferred: do no allow to display */
                        menu.findItem(R.id.menu_display_content).setVisible(false);
                    }
                }
            } else {
                // only image files are playable
                menu.findItem(R.id.menu_display_content).setVisible(false);
            }
        } else {
            // Only file are playable
            menu.findItem(R.id.menu_display_content).setVisible(false);
        }
        if (RcsService.Direction.OUTGOING != direction) {
            menu.findItem(R.id.menu_resend_message).setVisible(false);
            menu.findItem(R.id.menu_resend_via_xms).setVisible(false);
            return;
        }
        String id = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        try {
            switch (providerId) {
                case XmsMessageLog.HISTORYLOG_MEMBER_ID:
                    menu.findItem(R.id.menu_resend_message).setVisible(false);
                    menu.findItem(R.id.menu_resend_via_xms).setVisible(false);
                    break;

                case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                    menu.findItem(R.id.menu_resend_via_xms).setVisible(false);
                    menu.findItem(R.id.menu_resend_message).setVisible(false);
                    ChatLog.Message.Content.Status status = ChatLog.Message.Content.Status
                            .valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.STATUS)));
                    if (ChatLog.Message.Content.Status.FAILED == status) {
                        String number = cursor.getString(cursor
                                .getColumnIndexOrThrow(HistoryLog.CONTACT));
                        if (number != null) {
                            ContactId contact = ContactUtil.formatContact(number);
                            OneToOneChat chat = mChatService.getOneToOneChat(contact);
                            if (chat != null && chat.isAllowedToSendMessage()) {
                                menu.findItem(R.id.menu_resend_message).setVisible(true);
                            } else {
                                menu.findItem(R.id.menu_resend_via_xms).setVisible(true);
                            }
                        }
                    } else {
                        boolean expiredDelivery = cursor.getInt(cursor
                                .getColumnIndexOrThrow(HistoryLog.EXPIRED_DELIVERY)) == 1;
                        if (expiredDelivery) {
                            /* only allow resend via XMS if expired delivery */
                            menu.findItem(R.id.menu_resend_via_xms).setVisible(true);
                        }
                    }
                    break;

                case FileTransferLog.HISTORYLOG_MEMBER_ID:
                    menu.findItem(R.id.menu_resend_message).setVisible(false);
                    menu.findItem(R.id.menu_resend_via_xms).setVisible(false);
                    String mimeType = cursor.getString(cursor
                            .getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
                    FileTransfer.State state = FileTransfer.State.valueOf(cursor.getInt(cursor
                            .getColumnIndexOrThrow(HistoryLog.STATUS)));
                    if (FileTransfer.State.FAILED == state) {
                        FileTransfer transfer = mFileTransferService.getFileTransfer(id);
                        if (transfer != null && transfer.isAllowedToResendTransfer()) {
                            menu.findItem(R.id.menu_resend_message).setVisible(true);

                        } else if (mCanSendMms && Utils.isImageType(mimeType)) {
                            menu.findItem(R.id.menu_resend_via_xms).setVisible(true);
                        }
                    }
                    if (mCanSendMms && Utils.isImageType(mimeType)) {
                        boolean expiredDelivery = cursor.getInt(cursor
                                .getColumnIndexOrThrow(HistoryLog.EXPIRED_DELIVERY)) == 1;
                        if (expiredDelivery) {
                            menu.findItem(R.id.menu_resend_via_xms).setVisible(true);
                        }
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Invalid provider ID=" + providerId);
            }
        } catch (RcsServiceNotAvailableException e) {
            menu.findItem(R.id.menu_resend_message).setVisible(false);
            menu.findItem(R.id.menu_resend_via_xms).setVisible(true);

        } catch (RcsGenericException | RcsPersistentStorageException e) {
            menu.findItem(R.id.menu_resend_message).setVisible(false);
            menu.findItem(R.id.menu_resend_via_xms).setVisible(true);
            showException(e);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        String id = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected Id=".concat(id));
        }
        try {
            switch (item.getItemId()) {
                case R.id.menu_delete_message:
                    switch (providerId) {
                        case XmsMessageLog.HISTORYLOG_MEMBER_ID:
                            mCmsService.deleteXmsMessage(id);
                            break;

                        case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                            mChatService.deleteMessage(id);
                            break;

                        case FileTransferLog.HISTORYLOG_MEMBER_ID:
                            mFileTransferService.deleteFileTransfer(id);
                            break;

                        default:
                            throw new IllegalArgumentException("Invalid provider ID=" + providerId);
                    }
                    return true;

                case R.id.menu_resend_message:
                    switch (providerId) {
                        case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                            OneToOneChat chat = mChatService.getOneToOneChat(mContact);
                            if (chat != null) {
                                chat.resendMessage(id);
                            }
                            break;

                        case FileTransferLog.HISTORYLOG_MEMBER_ID:
                            FileTransfer fileTransfer = mFileTransferService.getFileTransfer(id);
                            if (fileTransfer != null) {
                                fileTransfer.resendTransfer();
                            }
                            break;

                        default:
                            throw new IllegalArgumentException("Invalid provider ID=" + providerId);
                    }
                    return true;

                case R.id.menu_resend_via_xms:
                    switch (providerId) {
                        case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                            resendChatMessageViaSms(id);
                            break;

                        case FileTransferLog.HISTORYLOG_MEMBER_ID:
                            resendFileTransferViaMms(id);
                            break;

                        default:
                            throw new IllegalArgumentException("Invalid provider ID=" + providerId);
                    }
                    return true;

                case R.id.menu_display_content:
                    switch (providerId) {
                        case FileTransferLog.HISTORYLOG_MEMBER_ID:
                            String file = cursor.getString(cursor
                                    .getColumnIndexOrThrow(HistoryLog.CONTENT));
                            Utils.showPicture(this, Uri.parse(file));
                            break;

                        default:
                            throw new IllegalArgumentException("Invalid provider ID=" + providerId);
                    }
                    return true;

                default:
                    return super.onContextItemSelected(item);
            }
        } catch (RcsGenericException | RcsPermissionDeniedException | RcsPersistentStorageException e) {
            showException(e);
            return true;

        } catch (RcsServiceNotAvailableException e) {
            Utils.displayLongToast(this, getString(R.string.label_service_not_available));
            return true;
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

    private void displayComposingEvent(final ContactId contact, final boolean status) {
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

    private void processUndeliveredFileTransfers(String displayName, boolean allowSwitchToMms)
            throws RcsGenericException, RcsServiceNotAvailableException,
            RcsPersistentStorageException, RcsPermissionDeniedException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processUndeliveredFileTransfers: ask");
        }
        /* Do not propose to clear undelivered if a dialog is already opened */
        if (mClearUndeliveredAlertDialog == null) {
            if (allowSwitchToMms) {
                mClearUndeliveredAlertDialog = popUpDeliveryExpiration(this,
                        getString(R.string.title_undelivered_filetransfer),
                        getString(R.string.label_undelivered_ft_resend_mms, displayName),
                        mResendFt, mDoNotResendFt, mUndeliveredCancelListener, true);
            } else {
                mClearUndeliveredAlertDialog = popUpDeliveryExpiration(this,
                        getString(R.string.title_undelivered_filetransfer),
                        getString(R.string.label_undelivered_filetransfer, displayName),
                        mDoNotResendFt, null, mUndeliveredCancelListener, false);
            }
            registerDialog(mClearUndeliveredAlertDialog);
        }
    }

    private void processUndeliveredMessages(String displayName) throws RcsGenericException,
            RcsPersistentStorageException, RcsServiceNotAvailableException {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processUndeliveredMessages: ask");
        }
        /* Do not propose to clear undelivered if a dialog is already opened */
        if (mClearUndeliveredAlertDialog == null) {
            mClearUndeliveredAlertDialog = popUpDeliveryExpiration(this,
                    getString(R.string.title_undelivered_message),
                    getString(R.string.label_undelivered_message, displayName), mResendChat,
                    mDoNotResendChat, mUndeliveredCancelListener, true);
            registerDialog(mClearUndeliveredAlertDialog);
        }
    }

    private AlertDialog popUpDeliveryExpiration(Context ctx, String title, String msg,
            DialogInterface.OnClickListener onPositiveClickListener,
            DialogInterface.OnClickListener onNegativeClickListener,
            DialogInterface.OnCancelListener onCancelListener, boolean saveChoice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setMessage(msg);
        builder.setTitle(title);
        if (onNegativeClickListener != null) {
            builder.setNegativeButton(R.string.label_cancel, onNegativeClickListener);
        }
        builder.setPositiveButton(R.string.label_ok, onPositiveClickListener);
        builder.setOnCancelListener(onCancelListener);
        if (saveChoice) {
            View checkBoxView = View.inflate(this, R.layout.cms_save_switch_xms, null);
            CheckBox checkBox = (CheckBox) checkBoxView
                    .findViewById(R.id.checkbox_cms_save_switch_xms);
            mSaveDoNotAskAgainResend = false;
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mSaveDoNotAskAgainResend = isChecked;
                }
            });
            builder.setView(checkBoxView);
        }
        return builder.show();
    }

    private void setRcsMode(boolean mode) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "setButtonSendMode to RCS: " + mode);
        }
        mRcsMode = mode;
        mSendBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, mode ? R.drawable.action_send_rcs
                : R.drawable.action_send_xms, 0);
    }

    private String getMyDisplayName() throws RcsGenericException, RcsServiceNotAvailableException {
        CommonServiceConfiguration config = mChatService.getCommonConfiguration();
        String myDisplayName = config.getMyDisplayName();
        if (myDisplayName == null) {
            myDisplayName = config.getMyContactId().toString();
        }
        return myDisplayName;
    }

    private void resendChatMessageViaSms(String messageId) throws RcsServiceNotAvailableException,
            RcsGenericException, RcsPersistentStorageException {
        ChatMessage chatMessage = mChatService.getChatMessage(messageId);
        String content = chatMessage.getContent();
        mCmsService.sendTextMessage(mContact, content);
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Re-send via SMS message ID=".concat(messageId));
        }
        mChatService.deleteMessage(messageId);
    }

    private void resendFileTransferViaMms(String fileTransferId) throws RcsGenericException,
            RcsServiceNotAvailableException, RcsPersistentStorageException,
            RcsPermissionDeniedException {
        FileTransfer fileTransfer = mFileTransferService.getFileTransfer(fileTransferId);
        if (fileTransfer == null) {
            throw new IllegalArgumentException("File transfer ID=" + fileTransferId + " not found!");
        }
        List<Uri> files = new ArrayList<>();
        Uri file = fileTransfer.getFile();
        if (!Utils.isImageType(fileTransfer.getMimeType())) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot resend via MMS transfer ID=" + fileTransferId
                        + ": not image!");
            }
            return;
        }
        takePersistableContentUriPermission(this, file);
        files.add(file);
        final String subject = getString(R.string.switch_mms_subject, getMyDisplayName());
        final String body = getString(R.string.switch_mms_body);
        SendMmsInBackground sendMmsTask = new SendMmsInBackground(mCmsService, mContact, files,
                subject, body, new SendMmsInBackground.TaskCompleted() {
                    @Override
                    public void onTaskComplete(final Exception result) {
                        if (result != null) {
                            OneToOneTalkView.this.showExceptionThenExit(result);
                        }
                    }
                });
        sendMmsTask.execute();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Re-send via MMS transfer ID=".concat(fileTransferId));
        }
        mFileTransferService.deleteFileTransfer(fileTransferId);
    }

    private void requestCapabilities(ContactId contact) throws RcsServiceNotAvailableException,
            RcsGenericException {
        try {
            mCapabilityService.requestContactCapabilities(new HashSet<>(Collections
                    .singletonList(contact)));

        } catch (RcsServiceNotRegisteredException e) {
            Log.w(LOGTAG, "Cannot request capabilities: RCS not registered!");
        }
    }

    private void addCmsServiceListeners() {
        if (!isServiceConnected(RcsServiceName.CMS)) {
            return;
        }
        if (!mXmsMessageListenerSet) {
            try {
                mCmsService.addEventListener(mXmsMessageListener);
                mXmsMessageListenerSet = true;
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }

        if (!mCmsSynchronizationListenerSet) {
            try {
                mCmsService.addEventListener(mCmsSynchronizationListener);
                mCmsSynchronizationListenerSet = true;
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }

    private void removeCmsServiceListeners() {
        if (!isServiceConnected(RcsServiceName.CMS)) {
            return;
        }
        if (mXmsMessageListenerSet) {
            try {
                mCmsService.removeEventListener(mXmsMessageListener);
                mXmsMessageListenerSet = false;
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }

        if (mCmsSynchronizationListenerSet) {
            try {
                mCmsService.removeEventListener(mCmsSynchronizationListener);
                mCmsSynchronizationListenerSet = false;
            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
    }
}
