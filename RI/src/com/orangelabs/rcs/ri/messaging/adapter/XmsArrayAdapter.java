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

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.cms.messaging.MmsPartDataObject;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.ImageUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by yplo6403 on 27/11/2015.
 */
public class XmsArrayAdapter extends ArrayAdapter<MmsPartDataObject> {

    private static final int MAX_IMAGE_WIDTH = 100;
    private static final int MAX_IMAGE_HEIGHT = 100;

    // TODO manage item selection listener to view image

    private final Context mCtx;
    private final int mResourceRowLayout;
    private final LayoutInflater mInflater;

    public XmsArrayAdapter(Context ctx, int resourceRowLayout, List<MmsPartDataObject> items) {
        super(ctx, 0, items);
        mCtx = ctx;
        mResourceRowLayout = resourceRowLayout;
        mInflater = LayoutInflater.from(mCtx);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ImageItemViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(mResourceRowLayout, parent, false);
            holder = new ImageItemViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ImageItemViewHolder) convertView.getTag();
        }
        MmsPartDataObject item = getItem(position);
        if (item != null) {
            holder.mFileNameText.setText(item.getFilename());
            holder.mFileSizeText
                    .setText(FileUtils.humanReadableByteCount(item.getFileSize(), true));
            Bitmap fileIcon = null;
            if (FileUtils.isImageType(item.getMimeType())) {
                String filePath = FileUtils.getPath(mCtx, item.getFile());
                if (filePath != null) {
                    // TODO do not perform on UI thread and use memory cache
                    fileIcon = ImageUtils.getImageBitmap2Display(filePath, MAX_IMAGE_WIDTH,
                            MAX_IMAGE_HEIGHT);
                }
            }
            if (fileIcon == null) {
                /* content has no thumbnail: display default thumbnail, filename and size */
                if (TalkCursorAdapter.sDefaultThumbnail == null) {
                    TalkCursorAdapter.sDefaultThumbnail = BitmapFactory.decodeResource(
                            mCtx.getResources(), R.drawable.video_file);
                }
                holder.mImageView.setImageBitmap(TalkCursorAdapter.sDefaultThumbnail);
            } else {
                holder.mImageView.setImageBitmap(fileIcon);
            }
        }
        return convertView;
    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById().
     */
    private class ImageItemViewHolder {
        public final ImageView mImageView;
        public final TextView mFileNameText;
        public final TextView mFileSizeText;

        ImageItemViewHolder(View base) {
            mImageView = (ImageView) base.findViewById(R.id.image);
            mFileNameText = (TextView) base.findViewById(R.id.FileName);
            mFileSizeText = (TextView) base.findViewById(R.id.FileSize);
        }
    }

}
