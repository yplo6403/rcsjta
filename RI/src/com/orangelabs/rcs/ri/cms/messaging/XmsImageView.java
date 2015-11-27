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

import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.IOException;

public class XmsImageView extends RcsActivity {

    private final static String EXTRA_IMAGE_URI = "imageUri";
    private final static String EXTRA_CONTACT = "contact";

    public static Intent forgeIntentToStart(Context context, MmsPartDataObject mmsPart) {
        Intent intent = new Intent(context, XmsImageView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_IMAGE_URI, mmsPart.getFile().toString());
        intent.putExtra(EXTRA_CONTACT, (Parcelable) mmsPart.getContact());
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xms_image_view);
        Intent intent = getIntent();
        Uri imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI));
        ContactId contact = intent.getParcelableExtra(EXTRA_CONTACT);
        setTitle(getString(R.string.title_send_mms, contact.toString()));
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
            ((ImageView) findViewById(R.id.xms_image_view)).setImageBitmap(scaled);
        } catch (IOException e) {
            showExceptionThenExit(e);
        }
    }
}
