/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
 ******************************************************************************/

package com.gsma.rcs.provider.cms;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.DatabaseUtils;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;

public class CmsProvider extends ContentProvider {

    /**
     * Database filename
     */
    /* package private */static final String DATABASE_NAME = "rcs_cms_imap.db";

    /**
     * Database table name
     */
    /* package private */static final String TABLE_FOLDER = "folder";
    /* package private */static final String TABLE_MESSAGE = "message";
    /**
     * Content provider URI
     */

    /* package private */static final int INVALID_ROW_ID = -1;

    private static final String SELECTION_WITH_ID_ONLY = BaseColumns._ID.concat("=?");

    private static final String SELECTION_WITH_FOLDER_NAME_ONLY = CmsFolder.KEY_NAME.concat("=?");

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(CmsFolder.CONTENT_URI.getAuthority(), CmsFolder.CONTENT_URI.getPath()
                .substring(1), UriType.Folder.FOLDER);
        sUriMatcher.addURI(CmsFolder.CONTENT_URI.getAuthority(), CmsFolder.CONTENT_URI.getPath()
                .substring(1).concat("/Default/*"), UriType.Folder.FOLDER_SINGLE);
        sUriMatcher.addURI(CmsFolder.CONTENT_URI.getAuthority(), CmsFolder.CONTENT_URI.getPath()
                .substring(1).concat("/Default/*/*"), UriType.Folder.FOLDER_GROUP);
        sUriMatcher.addURI(CmsObject.CONTENT_URI.getAuthority(), CmsObject.CONTENT_URI.getPath()
                .substring(1), UriType.Message.MESSAGE);
        sUriMatcher.addURI(CmsObject.CONTENT_URI.getAuthority(), CmsObject.CONTENT_URI.getPath()
                .substring(1).concat("/*"), UriType.Message.MESSAGE_WITH_ID);
    }

    private static final class UriType {

        private static final class Folder {
            private static final int FOLDER_SINGLE = 1;
            private static final int FOLDER_GROUP = 2;
            private static final int FOLDER = 3;
        }

        private static final class Message {
            private static final int MESSAGE_WITH_ID = 4;
            private static final int MESSAGE = 5;
        }
    }

    private static final class CursorType {

        private static final class Folder {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.rcs.cms.imap.folder";
            private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.rcs.cms.imap.folder";
        }

        private static final class Message {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.rcs.cms.imap.message";
            private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.rcs.cms.imap.message";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 2;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        // @formatter:off
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FOLDER + '(' + 
                    CmsFolder.KEY_NAME + " TEXT NOT NULL PRIMARY KEY," +
                    CmsFolder.KEY_NEXT_UID + " INTEGER," + 
                    CmsFolder.KEY_HIGHESTMODSEQ + " INTEGER," + 
                    CmsFolder.KEY_UID_VALIDITY + " INTEGER)");
            
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGE + '(' +
                    BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
                    CmsObject.KEY_FOLDER_NAME + " TEXT NOT NULL," +
                    CmsObject.KEY_UID + " INTEGER," + 
                    CmsObject.KEY_READ_STATUS + " INTEGER NOT NULL," + 
                    CmsObject.KEY_DELETE_STATUS + " INTEGER NOT NULL," + 
                    CmsObject.KEY_PUSH_STATUS + " INTEGER NOT NULL," + 
                    CmsObject.KEY_MESSAGE_TYPE + " TEXT NOT NULL," + 
                    CmsObject.KEY_MESSAGE_ID + " TEXT NOT NULL," + 
                    CmsObject.KEY_NATIVE_PROVIDER_ID + " INTEGER)");
            
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + CmsObject.KEY_FOLDER_NAME + "_idx"
                    + " ON " + TABLE_MESSAGE + '(' + CmsObject.KEY_FOLDER_NAME + ')');
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + CmsObject.KEY_FOLDER_NAME + "_"
                    + CmsObject.KEY_UID + "_idx" + " ON " + TABLE_MESSAGE + '('
                    + CmsObject.KEY_FOLDER_NAME + "," + CmsObject.KEY_UID + ')');
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + CmsObject.KEY_MESSAGE_ID + "_" +
                    CmsObject.KEY_MESSAGE_TYPE + "_idx" + " ON " + TABLE_MESSAGE +
                    '(' + CmsObject.KEY_MESSAGE_ID + "," + CmsObject.KEY_MESSAGE_TYPE + ')');
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + CmsObject.KEY_MESSAGE_ID + "_"
                    + CmsObject.KEY_UID + "_idx" + " ON " + TABLE_MESSAGE + '('
                    + CmsObject.KEY_MESSAGE_ID + "," + CmsObject.KEY_UID + ')');
        }
        // @formatter:on

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE_FOLDER));
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE_MESSAGE));
            onCreate(db);
        }

    }

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER:
                return CursorType.Folder.TYPE_DIRECTORY;
            case UriType.Folder.FOLDER_SINGLE:
            case UriType.Folder.FOLDER_GROUP:
                return CursorType.Folder.TYPE_ITEM;
            case UriType.Message.MESSAGE:
                return CursorType.Message.TYPE_DIRECTORY;
            case UriType.Message.MESSAGE_WITH_ID:
                return CursorType.Message.TYPE_ITEM;
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    private String getSelectionWithId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_ID_ONLY;
        }
        return "(" + SELECTION_WITH_ID_ONLY + ") AND (" + selection + ')';
    }

    private String getSelectionWithFolder(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_FOLDER_NAME_ONLY;
        }
        return "(" + SELECTION_WITH_FOLDER_NAME_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithId(String[] selectionArgs, String id) {
        String[] keySelectionArg = new String[] {
            id
        };
        if (selectionArgs == null) {
            return keySelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(keySelectionArg, selectionArgs);
    }

    private String[] getSelectionArgsWithTelUri(String[] selectionArgs, String telUri) {
        String[] keySelectionArg = new String[] {
            Constants.CMS_ROOT_DIRECTORY + Constants.CMS_DIRECTORY_SEPARATOR + telUri
        };
        if (selectionArgs == null) {
            return keySelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(keySelectionArg, selectionArgs);
    }

    private String[] getSelectionArgsWithChatId(String[] selectionArgs, String chatId) {
        String[] keySelectionArg = new String[] {
            com.gsma.rcs.core.cms.utils.CmsUtils.groupChatToCmsFolder(chatId, chatId)
        };
        if (selectionArgs == null) {
            return keySelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(keySelectionArg, selectionArgs);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER_GROUP:
                String chatId = uri.getLastPathSegment();
                selection = getSelectionWithFolder(selection);
                selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                return db.query(TABLE_FOLDER, projection, selection, selectionArgs, null, null,
                        sort);

            case UriType.Folder.FOLDER_SINGLE:
                String telUri = uri.getLastPathSegment();
                selection = getSelectionWithFolder(selection);
                selectionArgs = getSelectionArgsWithTelUri(selectionArgs, telUri);
                /* Intentional fall through */
            case UriType.Folder.FOLDER:
                db = mOpenHelper.getReadableDatabase();
                return db.query(TABLE_FOLDER, projection, selection, selectionArgs, null, null,
                        sort);

            case UriType.Message.MESSAGE_WITH_ID:
                String id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getReadableDatabase();
                return db.query(TABLE_MESSAGE, projection, selection, selectionArgs, null, null,
                        sort);
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER_GROUP:
                String chatId = uri.getLastPathSegment();
                selection = getSelectionWithFolder(selection);
                selectionArgs = getSelectionArgsWithChatId(selectionArgs, chatId);
                SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                return db.update(TABLE_FOLDER, values, selection, selectionArgs);

            case UriType.Folder.FOLDER_SINGLE:
                String telUri = uri.getLastPathSegment();
                selection = getSelectionWithFolder(selection);
                selectionArgs = getSelectionArgsWithTelUri(selectionArgs, telUri);
                /* Intentional fall through */
            case UriType.Folder.FOLDER:
                db = mOpenHelper.getReadableDatabase();
                return db.update(TABLE_FOLDER, values, selection, selectionArgs);

            case UriType.Message.MESSAGE_WITH_ID:
                String id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getReadableDatabase();
                return db.update(TABLE_MESSAGE, values, selection, selectionArgs);

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER_GROUP:
            case UriType.Folder.FOLDER_SINGLE:
                /* Intentional fall through */
            case UriType.Folder.FOLDER:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                db.insert(TABLE_FOLDER, null, initialValues);
                return uri;

            case UriType.Message.MESSAGE_WITH_ID:
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getWritableDatabase();
                if (db.insert(TABLE_MESSAGE, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri + '!');
                }
                return uri;
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER_GROUP:
                String chatId = uri.getLastPathSegment();
                where = getSelectionWithFolder(where);
                whereArgs = getSelectionArgsWithChatId(whereArgs, chatId);
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                return db.delete(TABLE_FOLDER, where, whereArgs);

            case UriType.Folder.FOLDER_SINGLE:
                String telUri = uri.getLastPathSegment();
                where = getSelectionWithFolder(where);
                whereArgs = getSelectionArgsWithTelUri(whereArgs, telUri);
                /* Intentional fall through */
            case UriType.Folder.FOLDER:
                db = mOpenHelper.getWritableDatabase();
                return db.delete(TABLE_FOLDER, where, whereArgs);

            case UriType.Message.MESSAGE_WITH_ID:
                String id = uri.getLastPathSegment();
                where = getSelectionWithId(where);
                whereArgs = getSelectionArgsWithId(whereArgs, id);
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getWritableDatabase();
                return db.delete(TABLE_MESSAGE, where, whereArgs);

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase database = mOpenHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            ContentProviderResult[] results = new ContentProviderResult[operations.size()];
            int index = 0;
            for (ContentProviderOperation operation : operations) {
                results[index] = operation.apply(this, results, index);
                index++;
            }
            database.setTransactionSuccessful();
            return results;
        } finally {
            database.endTransaction();
        }
    }
}
