/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010 France Telecom S.A.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

    private static Bitmap sDefaultThumbnail;
    private final Context mCtx;
    private int mSizeOf100Dp;
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
        mSizeOf100Dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, ctx
                .getResources().getDisplayMetrics());
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
                view.setTag(new SmsInViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_SMS_OUT:
                view = mInflater.inflate(R.layout.talk_item_sms_out, parent, false);
                view.setTag(new SmsOutViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_MMS_IN:
                view = mInflater.inflate(R.layout.talk_item_mms_in, parent, false);
                view.setTag(new MmsInViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_MMS_OUT:
                view = mInflater.inflate(R.layout.talk_item_mms_out, parent, false);
                view.setTag(new MmsOutViewHolder(view, cursor));
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
                bindMmsView(view, ctx, cursor);
                break;

            default:
                throw new IllegalArgumentException("Invalid view type: '" + viewType + "'!");
        }
    }

    private void bindSmsView(View view, Cursor cursor) {
        SmsViewHolder holder = (SmsViewHolder) view.getTag();
        // Set the date/time field by mixing relative and absolute times
        long date = cursor.getLong(holder.dateIdx);
        holder.mDate.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        holder.mContent.setText(cursor.getString(holder.contentIdx));
        holder.mStatus.setText(getXmsStatus(cursor, holder));
    }

    private void bindMmsView(View view, Context context, Cursor cursor) {
        MmsViewHolder holder = (MmsViewHolder) view.getTag();
        // Set the date/time field by mixing relative and absolute times
        long date = cursor.getLong(holder.dateIdx);
        holder.mDate.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        String mmsId = cursor.getString(holder.messageIdx);
        holder.mContent.setText(cursor.getString(holder.contentIdx));
        holder.mStatus.setText(getXmsStatus(cursor, holder));
        holder.mImagesLayout.removeAllViews();

        String status = RiApplication.sXmsMessageStates[(int) cursor.getLong(holder.statusIdx)];
        holder.mStatus.setText(status);

        boolean noThumbnail = false;

        for (MmsPartDataObject mmsPart : MmsPartDataObject.getParts(mCtx, mmsId)) {
            if (XmsMessageLog.MimeType.TEXT_MESSAGE.equals(mmsPart.getMimeType())) {
                /* discard mms body */
                continue;
            }
            byte[] fileIcon = mmsPart.getFileIcon();
            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(holder.imageParams);
            if (fileIcon == null) {
                /* content has no thumbnail: display default thumbnail, filename and size */
                if (sDefaultThumbnail == null) {
                    sDefaultThumbnail = BitmapFactory.decodeResource(mCtx.getResources(),
                            R.drawable.video_file);
                }
                imageView.setImageBitmap(sDefaultThumbnail);
                if (!noThumbnail) {
                    /*
                     * The filename and size is only displayed for the 1rst file with no thumbnail.
                     */
                    noThumbnail = true;
                    holder.mFilename.setVisibility(View.VISIBLE);
                    holder.mFilename.setText(mmsPart.getFilename());
                    holder.mFileSize.setVisibility(View.VISIBLE);
                    holder.mFileSize.setText(FileUtils.humanReadableByteCount(
                            mmsPart.getFileSize(), true));
                }
            } else {
                if (!noThumbnail) {
                    holder.mFilename.setVisibility(View.GONE);
                    holder.mFileSize.setVisibility(View.GONE);
                }
                imageView.setImageBitmap(BitmapFactory
                        .decodeByteArray(fileIcon, 0, fileIcon.length));
            }
            // TODO imageView.setOnClickListener(new ImageViewOnClickListener(mmsPart));
            holder.mImagesLayout.addView(imageView);
        }
    }

    private String getXmsStatus(Cursor cursor, SmsViewHolder holder) {
        XmsMessage.State state = XmsMessage.State.valueOf((int) cursor.getLong(holder.statusIdx));
        StringBuilder status = new StringBuilder(RiApplication.sXmsMessageStates[state.toInt()]);
        XmsMessage.ReasonCode reason = XmsMessage.ReasonCode.valueOf((int) cursor
                .getLong(holder.reasonIdx));
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

    private abstract class SmsViewHolder {
        RelativeLayout mItemLayout;
        TextView mContent;
        TextView mDate;
        TextView mStatus;
        int contentIdx;
        int dateIdx;
        int statusIdx;
        int reasonIdx;

        private SmsViewHolder(View view, Cursor cursor) {
            contentIdx = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
            dateIdx = cursor.getColumnIndexOrThrow(HistoryLog.TIMESTAMP);
            statusIdx = cursor.getColumnIndexOrThrow(HistoryLog.STATUS);
            reasonIdx = cursor.getColumnIndexOrThrow(HistoryLog.REASON_CODE);
            mContent = (TextView) view.findViewById(R.id.xms_content);
            mDate = (TextView) view.findViewById(R.id.xms_date);
            mStatus = (TextView) view.findViewById(R.id.xms_state);
        }
    }

    private class MmsViewHolder extends SmsViewHolder {
        int messageIdx;
        LinearLayout mImagesLayout;
        LinearLayout.LayoutParams imageParams;
        TextView mFilename;
        TextView mFileSize;

        private MmsViewHolder(View view, Cursor cursor) {
            super(view, cursor);
            messageIdx = cursor.getColumnIndexOrThrow(HistoryLog.ID);
            mFilename = (TextView) view.findViewById(R.id.mms_filename);
            mFileSize = (TextView) view.findViewById(R.id.mms_filesize);
            mImagesLayout = (LinearLayout) view.findViewById(R.id.xms_images_layout);
            imageParams = new LinearLayout.LayoutParams(mSizeOf100Dp, mSizeOf100Dp);
            imageParams.bottomMargin = mSizeOf100Dp / 10;
        }
    }

    private class SmsInViewHolder extends SmsViewHolder {
        private SmsInViewHolder(View view, Cursor cursor) {
            super(view, cursor);
            mItemLayout = (RelativeLayout) view.findViewById(R.id.conv_item_sms_in);
        }
    }

    private class SmsOutViewHolder extends SmsViewHolder {
        private SmsOutViewHolder(View view, Cursor cursor) {
            super(view, cursor);
            mItemLayout = (RelativeLayout) view.findViewById(R.id.conv_item_sms_out);
        }
    }

    private class MmsInViewHolder extends MmsViewHolder {

        private MmsInViewHolder(View view, Cursor cursor) {
            super(view, cursor);
            mItemLayout = (RelativeLayout) view.findViewById(R.id.conv_item_mms_in);
        }
    }

    private class MmsOutViewHolder extends MmsViewHolder {
        private MmsOutViewHolder(View view, Cursor cursor) {
            super(view, cursor);
            mItemLayout = (RelativeLayout) view.findViewById(R.id.conv_item_mms_out);
        }
    }

}
