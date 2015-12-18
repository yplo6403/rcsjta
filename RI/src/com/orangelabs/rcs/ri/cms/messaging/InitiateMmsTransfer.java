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

import com.gsma.services.rcs.cms.CmsService;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.adapter.XmsArrayAdapter;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Initiate XMS transfer
 *
 * @author Philippe LEMORDANT
 */
public class InitiateMmsTransfer extends RcsActivity {

    private final static String EXTRA_CONTACT = "contact";
    private static final String EXTRA_MIME_TYPE = "mimetype";

    private static final String SEND_MMS = "send_mms";

    private static final String LOGTAG = LogUtils.getTag(InitiateMmsTransfer.class.getSimpleName());

    private int PICK_IMAGE_REQUEST = 1;

    private ContactId mContact;
    private ListView mListView;
    private Button mSendBtn;
    private List<MmsPartDataObject> mMmsParts;
    private String[] mMimeType;

    /**
     * Starts the InitiateMmsTransfer activity
     *
     * @param ctx The context
     * @param contact The remote contact
     * @param mimeType The mime type to attach
     * @return intent
     */
    public static Intent forgeStartIntent(Context ctx, ContactId contact, String mimeType) {
        Intent intent = new Intent(ctx, InitiateMmsTransfer.class);
        intent.setAction(SEND_MMS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        intent.putExtra(EXTRA_MIME_TYPE, mimeType);
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
                EditText bodyText = (EditText) findViewById(R.id.messageEdit);
                String body = bodyText.getText().toString();
                EditText subjectText = (EditText) findViewById(R.id.subjectEdit);
                String subject = subjectText.getText().toString();
                List<Uri> files = new ArrayList<>();
                for (MmsPartDataObject mmsPart : mMmsParts) {
                    files.add(mmsPart.getFile());
                }
                /*
                 * The MMS sending is performed in background because the API returns a message
                 * instance only once it is persisted and to persist MMS, the core stack computes
                 * the file icon for image attached files.
                 */
                SendMmsTask sendMmsTask = new SendMmsTask(cmsService, mContact, files, subject,
                        body, new SendMmsTask.TaskCompleted() {
                            @Override
                            public void onTaskComplete(final Exception result) {
                                if (result != null) {
                                    InitiateMmsTransfer.this.showExceptionThenExit(result);
                                } else {
                                    Utils.displayLongToast(InitiateMmsTransfer.this,
                                            getString(R.string.mms_sending, mContact.toString()));
                                    InitiateMmsTransfer.this.finish();
                                }
                            }
                        });
                sendMmsTask.execute();
            }
        });

        Button selectParts = (Button) findViewById(R.id.SelectPartButton);
        selectParts.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setTitle(getString(R.string.title_send_mms, mContact.toString()));
                FileUtils.openFiles(InitiateMmsTransfer.this, mMimeType, PICK_IMAGE_REQUEST);
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
        mMimeType = new String[] {
            intent.getStringExtra(EXTRA_MIME_TYPE)
        };
        mContact = contact;
        setTitle(getString(R.string.title_send_mms, mContact.toString()));
        FileUtils.openFiles(this, mMimeType, PICK_IMAGE_REQUEST);
    }

    private void addImagePart( List<MmsPartDataObject> mmsParts, ContactId contact, Uri uri) {
        String filename = FileUtils.getFileName(this,uri);
        Long fileSize = FileUtils.getFileSize(this,uri);
        String mimeType = FileUtils.getMimeType(filename);
        if (mimeType != null && FileUtils.isImageType(mimeType)) {
            takePersistableContentUriPermission(this, uri);
            mmsParts.add(new MmsPartDataObject(mimeType, uri,  filename,fileSize, contact));
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            mMmsParts = new ArrayList<>();
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    addImagePart(mMmsParts, mContact, item.getUri());
                }
            } else {
                Uri uri = data.getData();
                if (uri != null) {
                    addImagePart(mMmsParts, mContact, uri);
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
        }
    }

    private static class SendMmsTask extends AsyncTask<Void, Void, Exception> {
        private final CmsService mCmsService;
        private final ContactId mContact;
        private final List<Uri> mFiles;
        private final String mSubject;
        private final String mBody;
        private final TaskCompleted mTaskCompleted;

        public SendMmsTask(CmsService cmsService, ContactId contact, List<Uri> files,
                String subject, String body, TaskCompleted taskCompleted) {
            mCmsService = cmsService;
            mContact = contact;
            mFiles = files;
            mSubject = subject;
            mBody = body;
            mTaskCompleted = taskCompleted;
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mCmsService.sendMultimediaMessage(mContact, mFiles, mSubject, mBody);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (mTaskCompleted != null) {
                mTaskCompleted.onTaskComplete(result);
            }
        }

        public interface TaskCompleted {
            void onTaskComplete(Exception result);
        }
    }
}
