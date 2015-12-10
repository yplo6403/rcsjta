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

package com.orangelabs.rcs.ri.contacts;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.contact.ContactUtil;

import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Display contact VCard
 * 
 * @author Jean-Marc AUFFRET
 */
public class ContactVCard extends RcsActivity {

    private static final String ACTION_VIEW_VCARD = "com.orangelabs.rcs.ri.contacts.ACTION_VIEW_VCARD";
    private static final String ACTION_SELECT_VCARD = "com.orangelabs.rcs.ri.contacts.ACTION_SELECT_VCARD";

    public static final String EXTRA_VCARD = "vcard";
    public static final String EXTRA_CONTACT = "contact";

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    boolean mActionView;
    private ContactUtil mContactUtil;
    private OnItemSelectedListener mListenerContact;
    private OnClickListener mBtnShowListener;
    private Button mSelectBtn;
    private OnClickListener mBtnSelListener;
    private String mContact;
    private Uri mVCardUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_vcard);

        initialize();

        /* Set the contact selector */
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this));
        mSpinner.setOnItemSelectedListener(mListenerContact);

        /* Set button callback */
        Button showBtn = (Button) findViewById(R.id.show_btn);
        showBtn.setOnClickListener(mBtnShowListener);

        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(mBtnSelListener);
        mSelectBtn.setVisibility(View.INVISIBLE);

        mContactUtil = ContactUtil.getInstance(this);

        processIntent(getIntent());
    }

    private void processIntent(Intent intent) {
        mActionView = ACTION_VIEW_VCARD.equals(intent.getAction());
    }

    private void initialize() {
        mListenerContact = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mContact = getSelectedContact();
                displayVisitCardUri(mContact);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
        mBtnShowListener = new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(mVCardUri, "text/plain");
                startActivity(intent);
            }
        };
        mBtnSelListener = new OnClickListener() {
            public void onClick(View v) {
                Intent in = new Intent();
                in.putExtra(EXTRA_VCARD, mVCardUri);
                in.putExtra(EXTRA_CONTACT, mContact);
                setResult(Activity.RESULT_OK, in);
                finish();
            }
        };
    }

    /**
     * Returns the selected contact
     * 
     * @return Contact
     */
    private String getSelectedContact() {
        // get selected phone number
        ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
        return adapter.getSelectedNumber(mSpinner.getSelectedView());
    }

    private Uri getVisitCard(String contact) throws RcsGenericException {
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(contact));
        return mContactUtil.getVCard(contactUri);
    }

    /**
     * Display the visit card
     * 
     * @param contact Contact
     */
    private void displayVisitCardUri(String contact) {
        try {
            mVCardUri = getVisitCard(contact);
            if (!mActionView) {
                mSelectBtn.setVisibility(View.VISIBLE);
            }
            TextView vcardView = (TextView) findViewById(R.id.vcard);
            vcardView.setText(mVCardUri.getPath());
        } catch (RcsGenericException e) {
            showExceptionThenExit(e);
        }
    }

    public static Intent forgeIntentViewVcard(Context ctx) {
        Intent intent = new Intent(ctx, ContactVCard.class);
        intent.setAction(ACTION_VIEW_VCARD);
        return intent;
    }

    public static Intent forgeIntentSelectVcard(Context ctx) {
        Intent intent = new Intent(ctx, ContactVCard.class);
        intent.setAction(ACTION_SELECT_VCARD);
        return intent;
    }

}
