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

package com.orangelabs.rcs.ri.messaging.adapter;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.cms.messaging.MmsPartDataObject;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Conversation cursor adapter
 */
public class TalkCursorAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_SMS_IN = 0;
    private static final int VIEW_TYPE_SMS_OUT = 1;
    private static final int VIEW_TYPE_MMS_IN = 2;
    private static final int VIEW_TYPE_MMS_OUT = 3;

    private static final String LOGTAG = LogUtils.getTag(TalkCursorAdapter.class.getSimpleName());

    public static Bitmap sDefaultThumbnail;
    private final Context mCtx;
    private LayoutInflater mInflater;

    /**
     * Constructor
     *
     * @param ctx The context
     */
    public TalkCursorAdapter(Context ctx) {
        super(ctx, null, 0);
        mCtx = ctx;
        mInflater = LayoutInflater.from(ctx);
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "ConversationCursorAdapter create");
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor);
        switch (viewType) {
            case VIEW_TYPE_SMS_IN:
                View view = mInflater.inflate(R.layout.talk_item_sms_in, parent, false);
                view.setTag(new SmsViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_SMS_OUT:
                view = mInflater.inflate(R.layout.talk_item_sms_out, parent, false);
                view.setTag(new SmsViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_MMS_IN:
                view = mInflater.inflate(R.layout.talk_item_mms_in, parent, false);
                view.setTag(new MmsViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_MMS_OUT:
                view = mInflater.inflate(R.layout.talk_item_mms_out, parent, false);
                view.setTag(new MmsViewHolder(view, cursor));
                return view;

            default:
                throw new IllegalArgumentException("Invalid view type: '" + viewType + "'!");
        }
    }

    @Override
    public void bindView(View view, Context ctx, Cursor cursor) {
        int viewType = getItemViewType(cursor);
        switch (viewType) {
            case VIEW_TYPE_SMS_IN:
            case VIEW_TYPE_SMS_OUT:
                bindSmsView(view, cursor);
                break;

            case VIEW_TYPE_MMS_IN:
            case VIEW_TYPE_MMS_OUT:
                bindMmsView(view, cursor);
                break;

            default:
                throw new IllegalArgumentException("Invalid view type: '" + viewType + "'!");
        }
    }

    private void bindSmsView(View view, Cursor cursor) {
        SmsViewHolder holder = (SmsViewHolder) view.getTag();
        // Set the date/time field by mixing relative and absolute times
        long date = cursor.getLong(holder.getColumnTimestampIdx());
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        holder.getContentText().setText(cursor.getString(holder.getColumnContentIdx()));
        holder.getStatusText().setText(getXmsStatus(cursor, holder));
    }

    private void bindMmsView(View view, Cursor cursor) {
        MmsViewHolder holder = (MmsViewHolder) view.getTag();
        // Set the date/time field by mixing relative and absolute times
        long date = cursor.getLong(holder.getColumnTimestampIdx());
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        String mmsId = cursor.getString(holder.getColumnIdIdx());
        holder.getContentText().setText(cursor.getString(holder.getColumnContentIdx()));
        holder.getStatusText().setText(getXmsStatus(cursor, holder));
        holder.getImagesLayout().removeAllViews();

        /*
         * A ListView (ScrollView) in a Listview (ScrollView) isn't possible on Android without
         * major bugs, Google says to not do it. You could have a LinearLayout inside of your list
         * item (with its orientation set to vertical) and manually use addView() to add views to
         * the LinearLayout that will be displayed with the layout's set orientation.
         */
        for (MmsPartDataObject mmsPart : MmsPartDataObject.getParts(mCtx, mmsId)) {
            String mimeTye = mmsPart.getMimeType();
            if (XmsMessageLog.MimeType.TEXT_MESSAGE.equals(mimeTye)
                    || XmsMessageLog.MimeType.APPLICATION_SMIL.equals(mimeTye)) {
                /* discard mms body or application/smil content */
                continue;
            }
            byte[] fileIcon = mmsPart.getFileIcon();
            View mmsItemView = mInflater.inflate(R.layout.mms_list_item, holder.getImagesLayout(),
                    false);
            ImageView imageView = (ImageView) mmsItemView.findViewById(R.id.image);
            TextView filenameText = (TextView) mmsItemView.findViewById(R.id.FileName);
            TextView fileSizeText = (TextView) mmsItemView.findViewById(R.id.FileSize);
            if (fileIcon == null) {
                /* content has no thumbnail: display default thumbnail, filename and size */
                if (sDefaultThumbnail == null) {
                    sDefaultThumbnail = BitmapFactory.decodeResource(mCtx.getResources(),
                            R.drawable.video_file);
                }
                imageView.setImageBitmap(sDefaultThumbnail);
                filenameText.setVisibility(View.VISIBLE);
                filenameText.setText(mmsPart.getFilename());
                fileSizeText.setVisibility(View.VISIBLE);
                fileSizeText.setText(FileUtils.humanReadableByteCount(mmsPart.getFileSize(), true));
            } else {
                imageView.setImageBitmap(BitmapFactory
                        .decodeByteArray(fileIcon, 0, fileIcon.length));
                filenameText.setVisibility(View.GONE);
                fileSizeText.setVisibility(View.GONE);
            }
            // TODO imageView.setOnClickListener(new ImageViewOnClickListener(mmsPart));
            holder.getImagesLayout().addView(mmsItemView);
        }
    }

    private String getXmsStatus(Cursor cursor, BasicViewHolder holder) {
        XmsMessage.State state = XmsMessage.State.valueOf((int) cursor.getLong(holder
                .getColumnStatusIdx()));
        StringBuilder status = new StringBuilder(RiApplication.sXmsMessageStates[state.toInt()]);
        XmsMessage.ReasonCode reason = XmsMessage.ReasonCode.valueOf((int) cursor.getLong(holder
                .getColumnReasonCodeIdx()));
        if (XmsMessage.ReasonCode.UNSPECIFIED != reason) {
            status.append(" / ");
            status.append(RiApplication.sXmsMessageReasonCodes[reason.toInt()]);
        }
        return status.toString();
    }

    public int getItemViewType(Cursor cursor) {
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndex(HistoryLog.DIRECTION)));
        switch (mimeType) {
            case XmsMessageLog.MimeType.TEXT_MESSAGE:
                if (Direction.INCOMING == direction) {
                    return VIEW_TYPE_SMS_IN;
                } else {
                    return VIEW_TYPE_SMS_OUT;
                }

            case XmsMessageLog.MimeType.MULTIMEDIA_MESSAGE:
                if (Direction.INCOMING == direction) {
                    return VIEW_TYPE_MMS_IN;
                } else {
                    return VIEW_TYPE_MMS_OUT;
                }

            default:
                throw new IllegalArgumentException("Invalid mime type: '" + mimeType + "'!");
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItemViewType((Cursor) getItem(position));
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

}
