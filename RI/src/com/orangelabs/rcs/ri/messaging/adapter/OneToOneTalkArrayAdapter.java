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

package com.orangelabs.rcs.ri.messaging.adapter;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;

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
        OneToOneTalkArrayItem item = getItem(position);
        OneToOneTalkArrayItem.ViewHolder holder = (OneToOneTalkArrayItem.ViewHolder) view.getTag();
        setContact(holder, item.getContact());
        setTimestamp(holder, item.getTimestamp());
        String content = item.getContent();
        if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(item.getMimeType())) {
            content = OneToOneTalkCursorAdapter.formatGeolocation(mCtx,
                    new Geoloc(item.getContent()));
        }
        setContent(holder, content);
        setStatus(holder, item.getUnreadCount());
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

    private void setStatus(OneToOneTalkArrayItem.ViewHolder holder, int unReads) {
        TextView statusText = holder.getStatusText();
        if (unReads == 0) {
            statusText.setVisibility(View.INVISIBLE);
        } else {
            statusText.setVisibility(View.VISIBLE);
            String countUnReads = Integer.valueOf(unReads).toString();
            if (unReads <= 9) {
                countUnReads = " ".concat(countUnReads);
            }
            statusText.setText(countUnReads);
        }
    }

    private void setContact(OneToOneTalkArrayItem.ViewHolder holder, ContactId contact) {
        String displayName = mRcsContactUtil.getDisplayName(contact);
        holder.getContactText().setText(displayName);
    }

}
