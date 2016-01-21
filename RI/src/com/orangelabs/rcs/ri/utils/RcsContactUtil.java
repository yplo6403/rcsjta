/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange
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

package com.orangelabs.rcs.ri.utils;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.contact.RcsContact;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.ri.R;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * Utilities to manage the RCS display name
 * 
 * @author Philippe LEMORDANT
 */
public class RcsContactUtil {

    private static final int MAX_DISPLAY_NAME_IN_CACHE = 200;
    private static final String LOGTAG = LogUtils.getTag(RcsContactUtil.class.getSimpleName());

    private static volatile RcsContactUtil sInstance;
    private final ContentResolver mResolver;
    private ContactService mService;
    private final String mDefaultDisplayName;
    private LruCache<ContactId, String> mDisplayNameAndroidCache;

    private static final String[] PROJ_DISPLAY_NAME = new String[] {
        ContactsContract.PhoneLookup.DISPLAY_NAME
    };

    /**
     * Constructor
     * 
     * @param context the context
     */
    private RcsContactUtil(Context context) {
        mService = ConnectionManager.getInstance().getContactApi();
        mResolver = context.getContentResolver();
        mDefaultDisplayName = context.getString(R.string.label_no_contact);
        mDisplayNameAndroidCache = new LruCache<>(MAX_DISPLAY_NAME_IN_CACHE);
    }

    /**
     * Get an instance of RcsDisplayName.
     * 
     * @param context the context
     * @return the singleton instance.
     */
    public static RcsContactUtil getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (RcsContactUtil.class) {
            if (sInstance == null) {
                sInstance = new RcsContactUtil(context);
            }
            return sInstance;
        }
    }

    private String getDisplayNameFromAddressBook(ContactId contact) {
        /* First try to get it from cache */
        String displayName = mDisplayNameAndroidCache.get(contact);
        if (displayName != null) {
            return displayName;
        }
        /* Not found in cache: query the Android address book */
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(contact.toString()));
        Cursor cursor = null;
        try {
            cursor = mResolver.query(uri, PROJ_DISPLAY_NAME, null, null, null);
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToFirst()) {
                displayName = cursor.getString(cursor
                        .getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                /* Insert in cache */
                mDisplayNameAndroidCache.put(contact,displayName);
                return displayName;
            }
            return null;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Returns display name which can be displayed on UI
     * 
     * @param contact the contact
     * @return the display name
     */
    public String getDisplayName(ContactId contact) {
        if (contact == null) {
            return mDefaultDisplayName;
        }
        try {
            if (mService == null) {
                mService = ConnectionManager.getInstance().getContactApi();
            }
            RcsContact rcsContact = mService.getRcsContact(contact);
            if (rcsContact == null) {
                String displayName = getDisplayNameFromAddressBook(contact);
                if (displayName != null) {
                    return displayName;
                }
                /*
                 * Contact exists but is not RCS: returns the phone number.
                 */
                return contact.toString();
            }
            String displayName = rcsContact.getDisplayName();
            if (displayName == null) {
                displayName = getDisplayNameFromAddressBook(contact);
                if (displayName != null) {
                    return displayName;
                }
                return contact.toString();
            } else {
                return displayName;
            }
        } catch (RcsServiceNotAvailableException ignore) {
            String displayName = getDisplayNameFromAddressBook(contact);
            if (displayName != null) {
                return displayName;
            }
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
        /* By default display name is set to the MSISDN */
        return contact.toString();
    }

    /**
     * get RCS display in a String which can be displayed on UI
     * 
     * @param number the phone number
     * @return the name which can be displayed on UI
     */
    public String getDisplayName(String number) {
        if (number == null) {
            return mDefaultDisplayName;
        }
        if (!ContactUtil.isValidContact(number)) {
            return number;
        }
        ContactId contact = ContactUtil.formatContact(number);
        return getDisplayName(contact);
    }
}
