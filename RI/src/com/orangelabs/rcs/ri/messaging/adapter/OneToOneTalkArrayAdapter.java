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

package com.orangelabs.rcs.ri.messaging.adapter;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.cms.XmsMessageLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Conversation cursor adapter
 */
public class OneToOneTalkArrayAdapter extends ArrayAdapter<OneToOneTalkArrayItem> {

    private final RcsContactUtil mRcsContactUtil;
    private final LayoutInflater mInflater;
    private final Context mCtx;

    public OneToOneTalkArrayAdapter(Context context, List<OneToOneTalkArrayItem> messageLogs) {
        super(context, R.layout.xms_list, messageLogs);
        mCtx = context;
        mRcsContactUtil = RcsContactUtil.getInstance(context);
        mInflater = ((Activity) context).getLayoutInflater();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.talk_log_item, parent, false);
            convertView.setTag(new OneToOneTalkArrayItem.ViewHolder(convertView));
        }
        bindView(convertView, position);
        return convertView;
    }

    public void bindView(View view, int position) {
        OneToOneTalkArrayItem message = getItem(position);
        switch (message.getProviderId()) {
            case XmsMessageLog.HISTORYLOG_MEMBER_ID:
                if (XmsMessageLog.MimeType.TEXT_MESSAGE.equals(message.getMimeType())) {
                    bindSmsView(view, message);
                } else {
                    bindMmsView(view, message);
                }
                break;

            case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                bindRcsChatView(view, message);
                break;

            case FileTransferLog.HISTORYLOG_MEMBER_ID:
                bindRcsFileTransferView(view, message);
                break;

            default:
                throw new IllegalArgumentException("Invalid provider ID: '"
                        + message.getProviderId() + "'!");
        }
    }

    private void bindSmsView(View view, OneToOneTalkArrayItem item) {
        OneToOneTalkArrayItem.ViewHolder holder = (OneToOneTalkArrayItem.ViewHolder) view.getTag();
        setContact(holder, item.getContact(), R.string.label_cms_sms_contact,
                R.drawable.ri_message_chat);
        setTimestamp(holder, item.getTimestamp());
        setContent(holder, item.getContent());
        XmsMessage.State state = XmsMessage.State.valueOf(item.getState());
        XmsMessage.ReasonCode reason = XmsMessage.ReasonCode.valueOf(item.getReason());
        setStatus(holder, item.getDirection(), XmsMessage.State.FAILED == state,
                getXmsStatus(state, reason));
    }

    private void bindMmsView(View view, OneToOneTalkArrayItem item) {
        OneToOneTalkArrayItem.ViewHolder holder = (OneToOneTalkArrayItem.ViewHolder) view.getTag();
        setContact(holder, item.getContact(), R.string.label_cms_mms_contact,
                R.drawable.ri_filetransfer);
        setTimestamp(holder, item.getTimestamp());
        setContent(holder, item.getContent());
        XmsMessage.State state = XmsMessage.State.valueOf(item.getState());
        XmsMessage.ReasonCode reason = XmsMessage.ReasonCode.valueOf(item.getReason());
        setStatus(holder, item.getDirection(), XmsMessage.State.FAILED == state,
                getXmsStatus(state, reason));
    }

    private void bindRcsFileTransferView(View view, OneToOneTalkArrayItem item) {
        OneToOneTalkArrayItem.ViewHolder holder = (OneToOneTalkArrayItem.ViewHolder) view.getTag();
        setContact(holder, item.getContact(), R.string.label_rcs_ft_contact,
                R.drawable.ri_filetransfer);
        setTimestamp(holder, item.getTimestamp());
        StringBuilder progress = new StringBuilder(item.getFilename());
        long fileSize = item.getFileSize();
        long transferred = item.getTransferred();
        FileTransfer.State state = FileTransfer.State.valueOf(item.getState());
        String status = null;
        String content = null;
        if (fileSize != transferred) {
            content = progress.append(" : ").append(Utils.getProgressLabel(transferred, fileSize))
                    .toString();
        } else {
            FileTransfer.ReasonCode reasonCode = FileTransfer.ReasonCode.valueOf(item.getReason());
            if (FileTransfer.ReasonCode.UNSPECIFIED == reasonCode) {
                content = progress.append(" (")
                        .append(FileUtils.humanReadableByteCount(fileSize, true)).append(")")
                        .toString();
            }
            status = getRcsFileTransferStatus(state, reasonCode);
        }
        setContent(holder, content);
        setStatus(holder, item.getDirection(), FileTransfer.State.FAILED == state, status);
    }

    private void bindRcsChatView(View view, OneToOneTalkArrayItem item) {
        OneToOneTalkArrayItem.ViewHolder holder = (OneToOneTalkArrayItem.ViewHolder) view.getTag();
        setContact(holder, item.getContact(), R.string.label_rcs_chat_contact,
                R.drawable.ri_message_chat);
        setTimestamp(holder, item.getTimestamp());
        String content = item.getContent();
        if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(item.getMimeType())) {
            content = OneToOneTalkCursorAdapter.formatGeolocation(mCtx,
                    new Geoloc(item.getContent()));
        }
        setContent(holder, content);
        ChatLog.Message.Content.Status state = ChatLog.Message.Content.Status.valueOf(item
                .getState());
        ChatLog.Message.Content.ReasonCode reason = ChatLog.Message.Content.ReasonCode.valueOf(item
                .getReason());
        setStatus(holder, item.getDirection(), ChatLog.Message.Content.Status.FAILED == state,
                getRcsChatStatus(state, reason));
    }

    private void setContent(OneToOneTalkArrayItem.ViewHolder holder, String content) {
        holder.getContentText().setText(content != null ? content : "");
    }

    private void setTimestamp(OneToOneTalkArrayItem.ViewHolder holder, long timestamp) {
        /* Set the date/time field by mixing relative and absolute times */
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
    }

    private void setStatus(OneToOneTalkArrayItem.ViewHolder holder, Direction dir, boolean failed,
            String status) {
        TextView statusText = holder.getStatusText();
        statusText.setText(status != null ? status : "");
        if (Direction.OUTGOING == dir) {
            if (failed) {
                statusText.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ri_outgoing_call_failed, 0, 0, 0);
            } else {
                statusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ri_outgoing_call, 0,
                        0, 0);
            }
        } else {
            if (failed) {
                statusText.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ri_incoming_call_failed, 0, 0, 0);
            } else {
                statusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ri_incoming_call, 0,
                        0, 0);
            }
        }
    }

    private void setContact(OneToOneTalkArrayItem.ViewHolder holder, ContactId contact,
            int resString, int icon) {
        String displayName = mRcsContactUtil.getDisplayName(contact);
        TextView contactText = holder.getContactText();
        contactText.setText(mCtx.getString(resString, displayName));
        contactText.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
    }

    private String getRcsFileTransferStatus(FileTransfer.State state, FileTransfer.ReasonCode reason) {
        StringBuilder status = new StringBuilder(RiApplication.sFileTransferStates[state.toInt()]);
        if (FileTransfer.ReasonCode.UNSPECIFIED != reason) {
            status.append(" / ");
            status.append(RiApplication.sFileTransferReasonCodes[reason.toInt()]);
        }
        return status.toString();
    }

    private String getRcsChatStatus(ChatLog.Message.Content.Status state,
            ChatLog.Message.Content.ReasonCode reason) {
        StringBuilder status = new StringBuilder(RiApplication.sMessagesStatuses[state.toInt()]);
        if (ChatLog.Message.Content.ReasonCode.UNSPECIFIED != reason) {
            status.append(" / ");
            status.append(RiApplication.sMessageReasonCodes[reason.toInt()]);
        }
        return status.toString();
    }

    private String getXmsStatus(XmsMessage.State state, XmsMessage.ReasonCode reason) {
        StringBuilder status = new StringBuilder(RiApplication.sXmsMessageStates[state.toInt()]);
        if (XmsMessage.ReasonCode.UNSPECIFIED != reason) {
            status.append(" / ");
            status.append(RiApplication.sXmsMessageReasonCodes[reason.toInt()]);
        }
        return status.toString();
    }

}
