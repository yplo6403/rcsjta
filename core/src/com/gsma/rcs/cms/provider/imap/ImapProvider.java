/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms.provider.imap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.DatabaseUtils;

public class ImapProvider extends ContentProvider {

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

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(FolderData.CONTENT_URI.getAuthority(),
                FolderData.CONTENT_URI.getPath().substring(1), UriType.Folder.FOLDER);
        sUriMatcher.addURI(FolderData.CONTENT_URI.getAuthority(),
                FolderData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.Folder.FOLDER_WITH_ID);
        sUriMatcher.addURI(MessageData.CONTENT_URI.getAuthority(),
                MessageData.CONTENT_URI.getPath().substring(1), UriType.Message.MESSAGE);
        sUriMatcher.addURI(MessageData.CONTENT_URI.getAuthority(),
                MessageData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.Message.MESSAGE_WITH_ID);
    }

    private static final class UriType {

        private static final class Folder {
            private static final int FOLDER_WITH_ID = 1;
            private static final int FOLDER = 2;
        }

        private static final class Message {
            private static final int MESSAGE_WITH_ID = 3;
            private static final int MESSAGE = 4;
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
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FOLDER + '(' + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + FolderData.KEY_NAME + " TEXT NOT NULL," + FolderData.KEY_NEXT_UID + " INTEGER," + FolderData.KEY_HIGHESTMODSEQ + " INTEGER," + FolderData.KEY_UID_VALIDITY + " INTEGER)");
            db.execSQL("CREATE INDEX " + TABLE_FOLDER + '_' + BaseColumns._ID + "_idx" + " ON " + TABLE_FOLDER + '(' + BaseColumns._ID + ')');
            db.execSQL("CREATE INDEX " + TABLE_FOLDER + '_' + FolderData.KEY_NAME + "_idx" + " ON " + TABLE_FOLDER + '(' + FolderData.KEY_NAME + ')');

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGE + '(' + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + MessageData.KEY_FOLDER_NAME + " TEXT," + MessageData.KEY_UID + " INTEGER," + MessageData.KEY_READ_STATUS + " INTEGER NOT NULL," + MessageData.KEY_DELETE_STATUS + " INTEGER NOT NULL," + MessageData.KEY_PUSH_STATUS + " INTEGER NOT NULL," + MessageData.KEY_MESSAGE_TYPE + " TEXT NOT NULL," + MessageData.KEY_MESSAGE_ID + " TEXT NOT NULL," + MessageData.KEY_NATIVE_PROVIDER_ID + " INTEGER)");
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + BaseColumns._ID + "_idx" + " ON " + TABLE_MESSAGE + '(' + BaseColumns._ID + ')');
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + MessageData.KEY_FOLDER_NAME + "_idx" + " ON " + TABLE_MESSAGE + '(' + MessageData.KEY_FOLDER_NAME + ')');
            db.execSQL("CREATE INDEX " + TABLE_MESSAGE + '_' + MessageData.KEY_FOLDER_NAME + "_" + MessageData.KEY_UID + "_idx" + " ON " + TABLE_MESSAGE + '(' + MessageData.KEY_FOLDER_NAME + "," + MessageData.KEY_UID + ')');

            // TODO
            // define another index
        }

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
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER:
                return CursorType.Folder.TYPE_DIRECTORY;
            case UriType.Folder.FOLDER_WITH_ID:
                return CursorType.Folder.TYPE_ITEM;
            case UriType.Message.MESSAGE:
                return CursorType.Message.TYPE_DIRECTORY;
            case UriType.Message.MESSAGE_WITH_ID:
                return CursorType.Message.TYPE_ITEM;
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI " + uri + "!");
        }
    }

    private String getSelectionWithId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_ID_ONLY;
        }
        return "(" + SELECTION_WITH_ID_ONLY + ") AND (" + selection + ')';
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

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sort) {
        Cursor cursor = null;

        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER_WITH_ID:
                String id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Folder.FOLDER:
                SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                cursor = db.query(TABLE_FOLDER, projection, selection, selectionArgs, null, null,
                        sort);
                return cursor;
            case UriType.Message.MESSAGE_WITH_ID:
                id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getReadableDatabase();
                cursor = db.query(TABLE_MESSAGE, projection, selection, selectionArgs, null, null,
                        sort);
                return cursor;
            default:
                throw new IllegalArgumentException(
                        new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
        }

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER_WITH_ID:
                String id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Folder.FOLDER:
                SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                return db.update(TABLE_FOLDER, values, selection, selectionArgs);
            case UriType.Message.MESSAGE_WITH_ID:
                id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getReadableDatabase();
                return db.update(TABLE_MESSAGE, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException(
                        new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER_WITH_ID:
                /* Intentional fall through */
            case UriType.Folder.FOLDER:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                if (db.insert(TABLE_FOLDER, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException(
                            new StringBuilder("Unable to insert row for URI ").append(uri)
                                    .append('!').toString());
                }
                return uri;
            case UriType.Message.MESSAGE_WITH_ID:
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getWritableDatabase();
                if (db.insert(TABLE_MESSAGE, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException(
                            new StringBuilder("Unable to insert row for URI ").append(uri)
                                    .append('!').toString());
                }
                return uri;
            default:
                throw new IllegalArgumentException(
                        new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Folder.FOLDER_WITH_ID:
                /* Intentional fall through */
            case UriType.Folder.FOLDER:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int nb = db.delete(TABLE_FOLDER, where, whereArgs);
                if (nb == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException(
                            new StringBuilder("Unable to insert row for URI ").append(uri)
                                    .append('!').toString());
                }
                return nb;
            case UriType.Message.MESSAGE_WITH_ID:
                /* Intentional fall through */
            case UriType.Message.MESSAGE:
                db = mOpenHelper.getWritableDatabase();
                nb = db.delete(TABLE_MESSAGE, where, whereArgs);
                if (nb == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException(
                            new StringBuilder("Unable to insert row for URI ").append(uri)
                                    .append('!').toString());
                }
                return nb;
            default:
                throw new IllegalArgumentException(
                        new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
        }
    }
}
