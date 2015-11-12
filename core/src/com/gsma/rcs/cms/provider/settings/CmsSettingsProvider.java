/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.cms.provider.settings;

import com.gsma.rcs.utils.DatabaseUtils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;

public class CmsSettingsProvider extends ContentProvider {

    private static final String TABLE = "setting";

    private static final String SELECTION_WITH_KEY_ONLY = CmsSettingsData.KEY_KEY.concat("=?");

    /**
     * Database filename
     */
    public static final String DATABASE_NAME = "cms_settings.db";

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(CmsSettingsData.CONTENT_URI.getAuthority(), CmsSettingsData.CONTENT_URI
                .getPath().substring(1), UriType.SETTINGS);
        sUriMatcher.addURI(CmsSettingsData.CONTENT_URI.getAuthority(), CmsSettingsData.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.SETTINGS_WITH_KEY);
    }

    private static final class UriType {

        private static final int SETTINGS = 1;

        private static final int SETTINGS_WITH_KEY = 2;
    }

    private static final class CursorType {

        private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.rcs.cms.settings";

        private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.rcs.cms.settings";
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 115;

        /**
         * Add a parameter in the db
         * 
         * @param db Database
         * @param key Key
         * @param value Value
         */
        private void addParameter(SQLiteDatabase db, String key, String value) {
            ContentValues values = new ContentValues();
            values.put(CmsSettingsData.KEY_KEY, key);
            values.put(CmsSettingsData.KEY_VALUE, value);
            db.insertOrThrow(TABLE, null, values);
        }

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE).append('(')
                    .append(CmsSettingsData.KEY_KEY).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(CmsSettingsData.KEY_VALUE).append(" TEXT)").toString());

            /* Insert default values for parameters */
            addParameter(db, CmsSettingsData.CMS_IMAP_SERVER_ADDRESS,
                    CmsSettingsData.DEFAULT_CMS_IMAP_SERVER_ADDRESS);
            addParameter(db, CmsSettingsData.CMS_IMAP_USER_LOGIN,
                    CmsSettingsData.DEFAULT_CMS_IMAP_USER_LOGIN);
            addParameter(db, CmsSettingsData.CMS_IMAP_USER_PWD,
                    CmsSettingsData.DEFAULT_CMS_IMAP_USER_PWD);
            addParameter(db, CmsSettingsData.CMS_RCS_MESSAGE_FOLDER,
                    CmsSettingsData.DEFAULT_CMS_RCS_MESSAGE_FOLDER);            
            addParameter(db, CmsSettingsData.CMS_MY_NUMBER,
                    CmsSettingsData.DEFAULT_CMS_MY_NUMBER);
            addParameter(db, CmsSettingsData.CMS_PUSH_SMS,
                    CmsSettingsData.DEFAULT_CMS_PUSH_SMS);
            addParameter(db, CmsSettingsData.CMS_PUSH_MMS,
                    CmsSettingsData.DEFAULT_CMS_PUSH_MMS);
            addParameter(db, CmsSettingsData.CMS_UPDATE_FLAGS_WITH_IMAP_XMS,
                    CmsSettingsData.DEFAULT_CMS_UPDATE_FLAGS_WITH_IMAP_XMS);
            addParameter(db, CmsSettingsData.CMS_DEFAULT_DIRECTORY,
                    CmsSettingsData.DEFAULT_CMS_DEFAULT_DIRECTORY);
            addParameter(db, CmsSettingsData.CMS_DIRECTORY_SEPARATOR,
                    CmsSettingsData.DEFAULT_CMS_DIRECTORY_SEPARATOR);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            /* Get old data before deleting the table */
            Cursor oldDataCursor = db.query(TABLE, null, null, null, null, null, null);
            /* TODO: Handle cursor when null. */

            /*
             * Get all the pairs key/value of the old table to insert them back after update
             */
            ArrayList<ContentValues> valuesList = new ArrayList<ContentValues>();
            while (oldDataCursor.moveToNext()) {
                String key = null;
                String value = null;
                int index = oldDataCursor.getColumnIndex(CmsSettingsData.KEY_KEY);
                if (index != -1) {
                    key = oldDataCursor.getString(index);
                }
                index = oldDataCursor.getColumnIndex(CmsSettingsData.KEY_VALUE);
                if (index != -1) {
                    value = oldDataCursor.getString(index);
                }
                if (key != null && value != null) {
                    ContentValues values = new ContentValues();
                    values.put(CmsSettingsData.KEY_KEY, key);
                    values.put(CmsSettingsData.KEY_VALUE, value);
                    valuesList.add(values);
                }
            }
            oldDataCursor.close();

            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE));

            onCreate(db);

            /* Put the old values back when possible */
            for (ContentValues values : valuesList) {
                String[] selectionArgs = new String[] {
                    values.getAsString(CmsSettingsData.KEY_KEY)
                };
                db.update(TABLE, values, SELECTION_WITH_KEY_ONLY, selectionArgs);
            }
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    private String getSelectionWithKey(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_KEY_ONLY;
        }
        return new StringBuilder("(").append(SELECTION_WITH_KEY_ONLY).append(") AND (")
                .append(selection).append(')').toString();
    }

    private String[] getSelectionArgsWithKey(String[] selectionArgs, String key) {
        String[] keySelectionArg = new String[] {
            key
        };
        if (selectionArgs == null) {
            return keySelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(keySelectionArg, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.SETTINGS:
                return CursorType.TYPE_DIRECTORY;

            case UriType.SETTINGS_WITH_KEY:
                return CursorType.TYPE_ITEM;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sort) {
        Cursor cursor = null;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.SETTINGS_WITH_KEY:
                    String key = uri.getLastPathSegment();
                    selection = getSelectionWithKey(selection);
                    selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                case UriType.SETTINGS:
                    SQLiteDatabase database = mOpenHelper.getReadableDatabase();
                    cursor = database.query(TABLE, projection, selection, selectionArgs, null,
                            null, sort);
                    /* TODO: Handle cursor when null. */
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                default:
                    throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                            .append(uri).append("!").toString());
            }
        }
        /*
         * TODO: Do not catch, close cursor, and then throw same exception. Callers should handle
         * exception.
         */
        catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.SETTINGS_WITH_KEY:
                String key = uri.getLastPathSegment();
                selection = getSelectionWithKey(selection);
                selectionArgs = getSelectionArgsWithKey(selectionArgs, key);
                /* Intentional fall through */
                //$FALL-THROUGH$
            case UriType.SETTINGS:
                SQLiteDatabase database = mOpenHelper.getWritableDatabase();
                int count = database.update(TABLE, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return count;

            default:
                throw new IllegalArgumentException(new StringBuilder("Unsupported URI ")
                        .append(uri).append("!").toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        throw new UnsupportedOperationException(new StringBuilder("Cannot insert URI ").append(uri)
                .append("!").toString());
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new UnsupportedOperationException(new StringBuilder("Cannot delete URI ").append(uri)
                .append("!").toString());
    }
}
