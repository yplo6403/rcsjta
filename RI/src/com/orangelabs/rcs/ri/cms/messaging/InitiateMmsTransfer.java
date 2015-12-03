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

import static com.orangelabs.rcs.ri.utils.FileUtils.takePersistableContentUriPermission;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.adapter.XmsArrayAdapter;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Initiate XMS transfer
 *
 * @author Philippe LEMORDANT
 */
public class InitiateMmsTransfer extends RcsActivity {

    private final static String EXTRA_CONTACT = "contact";

    private static final String SEND_MMS = "send_mms";

    private static final String LOGTAG = LogUtils.getTag(InitiateMmsTransfer.class.getSimpleName());

    private int PICK_IMAGE_REQUEST = 1;

    private ContactId mContact;
    private ListView mListView;
    private Button mSendBtn;
    private List<MmsPartDataObject> mMmsParts;

    // TODO manage option menu to enable content reselection

    /**
     * Starts the InitiateMmsTransfer activity
     *
     * @param ctx The context
     * @param contact The remote contact
     * @return intent
     */
    public static Intent forgeStartIntent(Context ctx, ContactId contact) {
        Intent intent = new Intent(ctx, InitiateMmsTransfer.class);
        intent.setAction(SEND_MMS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // TODO manage screen rotation
        setContentView(R.layout.mms_init_transfer);
        intitialize();
        processIntent(getIntent());
    }

    private void intitialize() {
        mListView = (ListView) findViewById(R.id.ImageList);
        /* Set send button listener */
        mSendBtn = (Button) findViewById(R.id.SendButton);
        mSendBtn.setEnabled(false);
        mSendBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onClick send MMS");
                }
                CmsService cmsService = getCmsApi();
                if (!cmsService.isServiceConnected()) {
                    showMessageThenExit(R.string.label_service_not_available);
                    return;
                }
                EditText editText = (EditText) findViewById(R.id.messageEdit);
                String text = editText.getText().toString();
                List<Uri> files = new ArrayList<>();
                try {
                    for (MmsPartDataObject mmsPart : mMmsParts) {
                        files.add(mmsPart.getFile());
                    }
                    cmsService.sendMultimediaMessage(mContact, files, text);

                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }
            }
        });
    }

    private void processIntent(Intent intent) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "processIntent ".concat(intent.getAction()));
        }
        ContactId contact = intent.getParcelableExtra(EXTRA_CONTACT);
        if (contact == null) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot process intent: contact is null");
            }
            return;
        }
        mContact = contact;
        setTitle(getString(R.string.title_send_mms, mContact.toString()));
        String[] mimeTypes = {
                "image/*", "video/*"
        };
        FileUtils.openFiles(this, mimeTypes, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            try {
                mMmsParts = new ArrayList<>();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();
                            takePersistableContentUriPermission(this, uri);
                            mMmsParts.add(new MmsPartDataObject(this, uri, mContact));
                        }
                    } else {
                        Uri uri = data.getData();
                        if (uri != null) {
                            takePersistableContentUriPermission(this, uri);
                            mMmsParts.add(new MmsPartDataObject(this, uri, mContact));
                        } else {
                            return;
                        }
                    }
                } else {
                    Uri uri = data.getData();
                    if (uri != null) {
                        takePersistableContentUriPermission(this, uri);
                        mMmsParts.add(new MmsPartDataObject(this, uri, mContact));
                    } else {
                        return;
                    }
                }
                if (mMmsParts.isEmpty()) {
                    showMessage(R.string.err_select_file);
                    mListView.setAdapter(null);
                } else {
                    XmsArrayAdapter adapter = new XmsArrayAdapter(this, R.layout.mms_list_item,
                            mMmsParts);
                    mListView.setAdapter(adapter);
                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                            MmsPartDataObject mmsPart = (MmsPartDataObject) (parent.getAdapter())
                                    .getItem(pos);
                            String msg = getString(R.string.toast_mms_image, mmsPart.getFilename(),
                                    mContact.toString());
                            Utils.showPictureAndExit(InitiateMmsTransfer.this, mmsPart.getFile(),
                                    msg);
                        }

                    });
                }
                mSendBtn.setEnabled(true);

            } catch (IOException e) {
                showExceptionThenExit(e);
            }
        }
    }

}
