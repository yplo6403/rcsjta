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

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.cms.messaging.MmsPartDataObject;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

/**
 * Conversation cursor adapter
 */
public class TalkCursorAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_SMS_IN = 0;
    private static final int VIEW_TYPE_SMS_OUT = 1;
    private static final int VIEW_TYPE_MMS_IN = 2;
    private static final int VIEW_TYPE_MMS_OUT = 3;
    private static final int VIEW_TYPE_RCS_CHAT_TEXT_IN = 4;
    private static final int VIEW_TYPE_RCS_CHAT_TEXT_OUT = 5;
    private static final int VIEW_TYPE_RCS_CHAT_LOC_IN = 6;
    private static final int VIEW_TYPE_RCS_CHAT_LOC_OUT = 7;
    private static final int VIEW_TYPE_RCS_FILE_TRANSFER_IN = 8;
    private static final int VIEW_TYPE_RCS_FILE_TRANSFER_OUT = 9;

    private static final String LOGTAG = LogUtils.getTag(TalkCursorAdapter.class.getSimpleName());

    public static Bitmap sDefaultThumbnail;
    private final LinearLayout.LayoutParams mImageParams;
    private LayoutInflater mInflater;

    /**
     * Constructor
     *
     * @param ctx The context
     */
    public TalkCursorAdapter(Context ctx) {
        super(ctx, null, 0);
        mInflater = LayoutInflater.from(ctx);

        int size100Dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, mContext.getResources().getDisplayMetrics());
        mImageParams = new LinearLayout.LayoutParams(size100Dp, size100Dp);

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

            case VIEW_TYPE_RCS_CHAT_LOC_IN:
            case VIEW_TYPE_RCS_CHAT_TEXT_IN:
                view = mInflater.inflate(R.layout.talk_item_rcs_chat_in, parent, false);
                view.setTag(new RcsChatInViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_RCS_CHAT_LOC_OUT:
            case VIEW_TYPE_RCS_CHAT_TEXT_OUT:
                view = mInflater.inflate(R.layout.talk_item_rcs_chat_out, parent, false);
                view.setTag(new RcsChatOutViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_RCS_FILE_TRANSFER_IN:
                view = mInflater.inflate(R.layout.talk_item_rcs_file_transfer_in, parent, false);
                view.setTag(new RcsFileTransferInViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_RCS_FILE_TRANSFER_OUT:
                view = mInflater.inflate(R.layout.talk_item_rcs_file_transfer_out, parent, false);
                view.setTag(new RcsFileTransferOutViewHolder(view, cursor));
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

            case VIEW_TYPE_RCS_CHAT_LOC_OUT:
            case VIEW_TYPE_RCS_CHAT_TEXT_OUT:
                bindRcsChatOutView(view, cursor);
                break;

            case VIEW_TYPE_RCS_CHAT_LOC_IN:
            case VIEW_TYPE_RCS_CHAT_TEXT_IN:
                bindRcsChatView(view, cursor);
                break;

            case VIEW_TYPE_RCS_FILE_TRANSFER_IN:
                bindRcsFileTransferInView(view, cursor, ctx);
                break;

            case VIEW_TYPE_RCS_FILE_TRANSFER_OUT:
                bindRcsFileTransferOutView(view, cursor, ctx);
                break;

            default:
                throw new IllegalArgumentException("Invalid view type: '" + viewType + "'!");
        }
    }

    public int getItemViewType(Cursor cursor) {
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndex(HistoryLog.DIRECTION)));
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
        switch (providerId) {
            case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                switch (mimeType) {
                    case ChatLog.Message.MimeType.GEOLOC_MESSAGE:
                        if (Direction.INCOMING == direction) {
                            return VIEW_TYPE_RCS_CHAT_LOC_IN;
                        } else {
                            return VIEW_TYPE_RCS_CHAT_LOC_OUT;
                        }

                    case ChatLog.Message.MimeType.TEXT_MESSAGE:
                        if (Direction.INCOMING == direction) {
                            return VIEW_TYPE_RCS_CHAT_TEXT_IN;
                        } else {
                            return VIEW_TYPE_RCS_CHAT_TEXT_OUT;
                        }
                }
                throw new IllegalArgumentException("Invalid mime type: '" + mimeType + "'!");

            case XmsMessageLog.HISTORYLOG_MEMBER_ID:
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
                }
                throw new IllegalArgumentException("Invalid mime type: '" + mimeType + "'!");

            case FileTransferLog.HISTORYLOG_MEMBER_ID:
                if (Direction.INCOMING == direction) {
                    return VIEW_TYPE_RCS_FILE_TRANSFER_IN;
                } else {
                    return VIEW_TYPE_RCS_FILE_TRANSFER_OUT;
                }
        }
        throw new IllegalArgumentException("Invalid provider IDe: '" + providerId + "'!");
    }

    private void bindRcsFileTransferOutView(View view, Cursor cursor, Context ctx) {
        bindRcsFileTransferInView(view, cursor, ctx);
        RcsFileTransferOutViewHolder holder = (RcsFileTransferOutViewHolder) view.getTag();
        boolean undeliveredExpiration = cursor.getInt(holder.getColumnExpiredDeliveryIdx()) == 1;
        holder.getUndeliveredView().setVisibility(undeliveredExpiration ? View.VISIBLE : View.GONE);
    }

    private void bindRcsFileTransferInView(View view, Cursor cursor, Context ctx) {
        RcsFileTransferInViewHolder holder = (RcsFileTransferInViewHolder) view.getTag();
        // Set the date/time field by mixing relative and absolute times
        long date = cursor.getLong(holder.getColumnTimestampIdx());
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        String mimeType = cursor.getString(holder.getColumnMimetypeIdx());
        StringBuilder filename = new StringBuilder(cursor.getString(holder.getColumnFilenameIdx()));
        long filesize = cursor.getLong(holder.getColumnFilesizeIdx());
        long transferred = cursor.getLong(holder.getColumnTransferredIdx());
        ImageView imageView = holder.getFileImageView();
        if (filesize != transferred) {
            holder.getProgressText().setText(
                    filename.append(" : ").append(Utils.getProgressLabel(transferred, filesize))
                            .toString());
            imageView.setImageResource(R.drawable.ri_filetransfer_off);
        } else {
            int reason = cursor.getInt(holder.getColumnReasonCodeIdx());
            FileTransfer.ReasonCode reasonCode = FileTransfer.ReasonCode.valueOf(reason);
            if (FileTransfer.ReasonCode.UNSPECIFIED == reasonCode) {
                if (FileUtils.isImageType(mimeType)) {
                    String uri = cursor.getString(holder.getColumnContentIdx());
                    Uri file = Uri.parse(uri);
                    try {
                        // TODO manage cache + create thumbnail in background
                        byte[] fileIcon = FileUtils.createThumb(ctx.getContentResolver(), file);
                        imageView.setImageBitmap(BitmapFactory.decodeByteArray(fileIcon, 0, fileIcon.length));
                        imageView.setLayoutParams(mImageParams);

                    } catch (IOException e) {
                        holder.getFileImageView().setImageResource(R.drawable.ri_filetransfer_on);
                        Log.e(LOGTAG, "bindRcsFileTransferInView failed to display image URI " + uri, e);
                    }
                } else {
                    // TODO create thumbnail for video
                    imageView.setImageResource(R.drawable.ri_filetransfer_on);
                }
            } else {
                imageView.setImageResource(R.drawable.ri_filetransfer_off);
            }
            holder.getProgressText().setText(
                    filename.append(" (").append(FileUtils.humanReadableByteCount(filesize,true)).append(")").toString());
        }
        holder.getStatusText().setText(getRcsFileTransferStatus(cursor, holder));
    }

    private void bindRcsChatOutView(View view, Cursor cursor) {
        bindRcsChatView(view, cursor);
        RcsChatOutViewHolder holder = (RcsChatOutViewHolder) view.getTag();
        boolean undeliveredExpiration = cursor.getInt(holder.getColumnExpiredDeliveryIdx()) == 1;
        holder.getUndeliveredView().setVisibility(undeliveredExpiration ? View.VISIBLE : View.GONE);
    }

    private void bindRcsChatView(View view, Cursor cursor) {
        RcsChatInViewHolder holder = (RcsChatInViewHolder) view.getTag();
        // Set the date/time field by mixing relative and absolute times
        long date = cursor.getLong(holder.getColumnTimestampIdx());
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        String mimeType = cursor.getString(holder.getColumnMimetypeIdx());
        TextView contentText = holder.getContentText();
        String data = cursor.getString(holder.getColumnContentIdx());
        if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(mimeType)) {
            contentText.setText(data);
        } else {
            Geoloc geoloc = new Geoloc(data);
            contentText.setText(formatGeolocation(mContext, geoloc));
        }
        holder.getStatusText().setText(getRcsChatStatus(cursor, holder));
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
        for (MmsPartDataObject mmsPart : MmsPartDataObject.getParts(mContext, mmsId)) {
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
                    sDefaultThumbnail = BitmapFactory.decodeResource(mContext.getResources(),
                            R.drawable.video_file);
                }
                imageView.setImageBitmap(sDefaultThumbnail);
                filenameText.setVisibility(View.VISIBLE);
                filenameText.setText(mmsPart.getFilename());
                fileSizeText.setVisibility(View.VISIBLE);
                fileSizeText.setText(FileUtils.humanReadableByteCount(mmsPart.getFileSize(), true));
            } else {
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(fileIcon, 0, fileIcon.length));
                imageView.setLayoutParams(mImageParams);
                filenameText.setVisibility(View.GONE);
                fileSizeText.setVisibility(View.GONE);
            }
            // TODO imageView.setOnClickListener(new ImageViewOnClickListener(mmsPart));
            holder.getImagesLayout().addView(mmsItemView);
        }
    }

    private String getRcsFileTransferStatus(Cursor cursor, RcsFileTransferInViewHolder holder) {
        FileTransfer.State state = FileTransfer.State.valueOf((int) cursor.getLong(holder
                .getColumnStatusIdx()));
        StringBuilder status = new StringBuilder(RiApplication.sFileTransferStates[state.toInt()]);
        FileTransfer.ReasonCode reason = FileTransfer.ReasonCode.valueOf((int) cursor
                .getLong(holder.getColumnReasonCodeIdx()));
        if (FileTransfer.ReasonCode.UNSPECIFIED != reason) {
            status.append(" / ");
            status.append(RiApplication.sFileTransferReasonCodes[reason.toInt()]);
        }
        return status.toString();
    }

    private String getRcsChatStatus(Cursor cursor, RcsChatInViewHolder holder) {
        ChatLog.Message.Content.Status state = ChatLog.Message.Content.Status.valueOf((int) cursor
                .getLong(holder.getColumnStatusIdx()));
        StringBuilder status = new StringBuilder(RiApplication.sMessagesStatuses[state.toInt()]);
        ChatLog.Message.Content.ReasonCode reason = ChatLog.Message.Content.ReasonCode
                .valueOf((int) cursor.getLong(holder.getColumnReasonCodeIdx()));
        if (ChatLog.Message.Content.ReasonCode.UNSPECIFIED != reason) {
            status.append(" / ");
            status.append(RiApplication.sMessageReasonCodes[reason.toInt()]);
        }
        return status.toString();
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

    @Override
    public int getItemViewType(int position) {
        return getItemViewType((Cursor) getItem(position));
    }

    @Override
    public int getViewTypeCount() {
        return 10;
    }

    /**
     * Format geolocation
     *
     * @param context context
     * @param geoloc  The geolocation
     * @return a formatted text
     */
    private CharSequence formatGeolocation(Context context, Geoloc geoloc) {
        StringBuilder result = new StringBuilder(context.getString(R.string.label_geolocation_msg))
                .append("\n");
        String label = geoloc.getLabel();
        if (label != null) {
            result.append(context.getString(R.string.label_location)).append(" ")
                    .append(geoloc.getLabel()).append("\n");
        }
        return result.append(context.getString(R.string.label_latitude)).append(" ")
                .append(geoloc.getLatitude()).append("\n")
                .append(context.getString(R.string.label_longitude)).append(" ")
                .append(geoloc.getLongitude()).append("\n")
                .append(context.getString(R.string.label_accuracy)).append(" ")
                .append(geoloc.getAccuracy()).toString();
    }
}
