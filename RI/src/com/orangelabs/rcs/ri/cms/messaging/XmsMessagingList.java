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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageListener;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsFragmentActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Set;

/**
 * List XMS conversation from the content provider
 * 
 * @author YPLO6403
 */
public class XmsMessagingList extends RcsFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // @formatter:off
    private static final String[] PROJECTION = new String[] {
        XmsMessageLog.BASECOLUMN_ID,
        XmsMessageLog.CONTACT,
        XmsMessageLog.CONTENT,
        XmsMessageLog.MIME_TYPE,
        XmsMessageLog.TIMESTAMP,
        XmsMessageLog.READ_STATUS
    };
    // @formatter:on

    private static final String WHERE_CLAUSE_GROUPED = HistoryLog.CHAT_ID + "="
            + HistoryLog.CONTACT + " GROUP BY " + HistoryLog.CONTACT;

    private static final String SORT_ORDER = XmsMessageLog.TIMESTAMP + " DESC";

    private ListView mListView;

    private CmsService mCmsService;

    private boolean mXmsMessageListenerSet = false;

    private CmsListAdapter mAdapter;

    private Handler mHandler = new Handler();

    private static final String LOGTAG = LogUtils.getTag(XmsMessagingList.class.getSimpleName());

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    /**
     * List of items for contextual menu
     */
    private static final int CMS_MENU_ITEM_DELETE = 1;
    private static final int CMS_MENU_ITEM_OPEN = 0;

    private static final int MESSAGE_BODY_MAX_SIZE = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.xms_list);

        mCmsService = getCmsApi();

        mListView = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyView);
        registerForContextMenu(mListView);

        mAdapter = new CmsListAdapter(this);
        mListView.setAdapter(mAdapter);
        /*
         * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
         */
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCmsService == null || !mXmsMessageListenerSet) {
            return;
        }
        try {
            mCmsService.removeEventListener(mXmsMessageListener);

        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    /**
     * CMS list adapter
     */
    private class CmsListAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        /**
         * Constructor
         * 
         * @param context Context
         */
        public CmsListAdapter(Context context) {
            super(context, null, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mInflater.inflate(R.layout.xms_list_item, parent, false);
            view.setTag(new XmsMessageListItemViewHolder(view, cursor));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final XmsMessageListItemViewHolder holder = (XmsMessageListItemViewHolder) view
                    .getTag();
            long date = cursor.getLong(holder.columnTimestamp);
            holder.dateText.setText(DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));

            String number = cursor.getString(holder.columnContact);
            String displayName = RcsContactUtil.getInstance(context).getDisplayName(number);
            String mimetype = cursor.getString(holder.columnMimeType);
            if (XmsMessageLog.MimeType.TEXT_MESSAGE.equals(mimetype)) {
                holder.contactText.setText(getString(R.string.label_cms_sms_contact, displayName));
            } else {
                holder.contactText.setText(getString(R.string.label_cms_mms_contact, displayName));
            }
            String content = truncate(cursor.getString(holder.columnContent));
            holder.contentText.setText((content == null) ? "" : content);
            holder.contentText.setVisibility(View.VISIBLE);
            holder.showAsNew(cursor.getInt(holder.columnReadStatus) == 0);
        }
    }

    private String truncate(String content) {
        if (content != null && content.length() > MESSAGE_BODY_MAX_SIZE) {
            content = content.substring(0, MESSAGE_BODY_MAX_SIZE);
            content = content.concat("...");
        }
        return content;
    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById() or getColumnIndex() on each row.
     */
    private class XmsMessageListItemViewHolder {
        int columnContact;

        int columnContent;

        int columnMimeType;

        int columnTimestamp;

        int columnReadStatus;

        int defaultColor;

        TextView contactText;

        TextView contentText;

        TextView dateText;

        XmsMessageListItemViewHolder(View base, Cursor cursor) {
            columnContact = cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT);
            columnContent = cursor.getColumnIndexOrThrow(XmsMessageLog.CONTENT);
            columnMimeType = cursor.getColumnIndexOrThrow(XmsMessageLog.MIME_TYPE);
            columnTimestamp = cursor.getColumnIndexOrThrow(XmsMessageLog.TIMESTAMP);
            columnReadStatus = cursor.getColumnIndexOrThrow(XmsMessageLog.READ_STATUS);
            contactText = (TextView) base.findViewById(R.id.line1);
            contentText = (TextView) base.findViewById(R.id.line2);
            dateText = (TextView) base.findViewById(R.id.date);
            defaultColor = contactText.getTextColors().getDefaultColor();
        }

        void showAsNew(boolean isNew) {
            int color = isNew ? Color.GREEN : defaultColor;
            int style = isNew ? (Typeface.BOLD | Typeface.ITALIC) : Typeface.NORMAL;
            contactText.setTypeface(null, style);
            contactText.setTextColor(color);
            contentText.setTypeface(null, style);
            contentText.setTextColor(color);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_log:
                /* Delete all XMS messages */
                if (!isServiceConnected(RcsServiceName.CMS)) {
                    showMessage(R.string.label_service_not_available);
                    break;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "delete all XMS conversation");
                }
                try {
                    if (!mXmsMessageListenerSet) {
                        mCmsService.addEventListener(mXmsMessageListener);
                        mXmsMessageListenerSet = true;
                    }
                    mCmsService.deleteXmsMessages();

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                break;
            case R.id.menu_sync_xms:
                /* Start a sync with CMS */
                if (!isServiceConnected(RcsServiceName.CMS)) {
                    showMessage(R.string.label_service_not_available);
                    break;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "start a sync");
                }
                try {
                    mCmsService.syncAll();
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        /* Check CMS API is connected */
        if (!isServiceConnected(RcsServiceName.CMS)) {
            showMessage(R.string.label_service_not_available);
            return;
        }
        menu.add(0, CMS_MENU_ITEM_OPEN, CMS_MENU_ITEM_OPEN, R.string.menu_open_xms_talk);
        menu.add(0, CMS_MENU_ITEM_DELETE, CMS_MENU_ITEM_DELETE, R.string.menu_delete_xms_talk);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /* Get selected item */
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mListView.getAdapter()).getItem(info.position);
        String number = cursor.getString(cursor.getColumnIndexOrThrow(XmsMessageLog.CONTACT));
        ContactId contact = ContactUtil.formatContact(number);
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected contact=".concat(contact.toString()));
        }
        switch (item.getItemId()) {
            case CMS_MENU_ITEM_OPEN:
                if (isServiceConnected(RcsServiceName.CMS)) {
                    /* Open CMS conversation view */
                    startActivity(XmsView.forgeIntentToOpenConversation(this, contact));
                } else {
                    showMessage(R.string.err_continue_xms_failed);
                }
                return true;

            case CMS_MENU_ITEM_DELETE:
                if (!isServiceConnected(RcsServiceName.CMS)) {
                    showMessage(R.string.label_xms_delete_failed);
                    return true;
                }
                /* Delete messages for contact */
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Delete XMS messages for contact=".concat(contact.toString()));
                }
                try {
                    if (!mXmsMessageListenerSet) {
                        mCmsService.addEventListener(mXmsMessageListener);
                        mXmsMessageListenerSet = true;
                    }
                    mCmsService.deleteXmsMessages(contact);

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    private XmsMessageListener mXmsMessageListener = new XmsMessageListener() {

        @Override
        public void onDeleted(final ContactId contact, Set<String> msgIds) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        "onDeleted contact=" + contact + " for message IDs="
                                + Arrays.toString(msgIds.toArray()));
            }
            mHandler.post(new Runnable() {
                public void run() {
                    Utils.displayLongToast(XmsMessagingList.this,
                            getString(R.string.label_xms_delete_success, contact.toString()));
                }
            });
        }

        @Override
        public void onStateChanged(ContactId contact, String mimeType, String messageId,
                XmsMessage.State state, XmsMessage.ReasonCode reasonCode) {
        }

    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, XmsMessageLog.CONTENT_URI, PROJECTION, WHERE_CLAUSE_GROUPED,
                null, SORT_ORDER);
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
}
