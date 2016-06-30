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
 ******************************************************************************/

package com.gsma.rcs.provider.xms;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.history.HistoryMemberBaseIdCreator;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.DatabaseUtils;
import com.gsma.services.rcs.cms.MmsPartLog;
import com.gsma.services.rcs.cms.XmsMessageLog;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Philippe LEMORDANT
 */
@SuppressWarnings("ConstantConditions")
public class XmsProvider extends ContentProvider {

    /**
     * Database table for XMS messages
     */
    public static final String TABLE_XMS = "xms";
    /**
     * Database table for MMS parts
     */
    public static final String TABLE_PART = "part";
    /**
     * Database filename
     */
    public static final String DATABASE_NAME = "cms_xms.db";

    private static final int INVALID_ROW_ID = -1;

    private static final String SELECTION_WITH_PART_ID_ONLY = PartData.KEY_PART_ID.concat("=?");

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(XmsMessageLog.CONTENT_URI.getAuthority(), XmsMessageLog.CONTENT_URI
                .getPath().substring(1), UriType.Xms.XMS);

        sUriMatcher.addURI(MmsPartLog.CONTENT_URI.getAuthority(), MmsPartLog.CONTENT_URI.getPath()
                .substring(1), UriType.Part.PART);
        sUriMatcher.addURI(MmsPartLog.CONTENT_URI.getAuthority(), MmsPartLog.CONTENT_URI.getPath()
                .substring(1).concat("/*"), UriType.Part.PART_WITH_ID);

        sUriMatcher.addURI(XmsData.CONTENT_URI.getAuthority(), XmsData.CONTENT_URI.getPath()
                .substring(1), UriType.InternalXms.XMS);

        sUriMatcher.addURI(PartData.CONTENT_URI.getAuthority(), PartData.CONTENT_URI.getPath()
                .substring(1), UriType.InternalPart.PART);
        sUriMatcher.addURI(PartData.CONTENT_URI.getAuthority(), PartData.CONTENT_URI.getPath()
                .substring(1).concat("/*"), UriType.InternalPart.PART_WITH_ID);
    }

    /**
     * Strings to allow projection for exposed URI to a set of columns.
     */
    private static final String[] PARTS_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS = new String[] {
            PartData.KEY_PART_ID, PartData.KEY_MESSAGE_ID, XmsData.KEY_CONTACT,
            PartData.KEY_CONTENT, PartData.KEY_MIME_TYPE, PartData.KEY_FILENAME,
            PartData.KEY_FILESIZE, PartData.KEY_FILEICON
    };

    private static final Set<String> PARTS_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS = new HashSet<>(
            Arrays.asList(PARTS_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS));

    /**
     * Strings to allow projection for exposed URI to a set of columns.
     */
    private static final String[] XMS_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS = new String[] {
            XmsData.KEY_CHAT_ID, XmsData.KEY_BASECOLUMN_ID, XmsData.KEY_MESSAGE_ID,
            XmsData.KEY_CONTACT, XmsData.KEY_CONTENT, XmsData.KEY_MIME_TYPE, XmsData.KEY_DIRECTION,
            XmsData.KEY_TIMESTAMP, XmsData.KEY_TIMESTAMP_SENT, XmsData.KEY_TIMESTAMP_DELIVERED,
            XmsData.KEY_STATE, XmsData.KEY_REASON_CODE, XmsData.KEY_READ_STATUS
    };

    private static final Set<String> XMS_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS = new HashSet<>(
            Arrays.asList(XMS_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS));

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Xms.XMS:
                /* Intentional fall through */
            case UriType.InternalXms.XMS:
                return CursorType.Xms.TYPE_DIRECTORY;

            case UriType.Part.PART:
                /* Intentional fall through */
            case UriType.InternalPart.PART:
                return CursorType.Part.TYPE_DIRECTORY;

            case UriType.Part.PART_WITH_ID:
                /* Intentional fall through */
            case UriType.InternalPart.PART_WITH_ID:
                return CursorType.Part.TYPE_ITEM;

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    private String getSelectionWithPartId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_PART_ID_ONLY;
        }
        return "(" + SELECTION_WITH_PART_ID_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithPartId(String[] selectionArgs, String id) {
        return DatabaseUtils.appendIdWithSelectionArgs(id, selectionArgs);
    }

    private String[] restrictPartsProjectionToExternallyDefinedColumns(String[] projection)
            throws UnsupportedOperationException {
        if (projection == null || projection.length == 0) {
            return PARTS_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS;
        }
        for (String projectedColumn : projection) {
            if (!PARTS_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS.contains(projectedColumn)) {
                throw new UnsupportedOperationException("No visibility to the accessed column "
                        + projectedColumn + "!");
            }
        }
        return projection;
    }

    private String[] restrictXmsProjectionToExternallyDefinedColumns(String[] projection)
            throws UnsupportedOperationException {
        if (projection == null || projection.length == 0) {
            return XMS_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS;
        }
        for (String projectedColumn : projection) {
            if (!XMS_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS.contains(projectedColumn)) {
                throw new UnsupportedOperationException("No visibility to the accessed column "
                        + projectedColumn + "!");
            }
        }
        return projection;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        Cursor cursor = null;
        try {
            switch (sUriMatcher.match(uri)) {

                case UriType.InternalXms.XMS:
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_XMS, projection, selection, selectionArgs, null, null,
                            sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            XmsMessageLog.CONTENT_URI);
                    return cursor;

                case UriType.Xms.XMS:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_XMS,
                            restrictXmsProjectionToExternallyDefinedColumns(projection), selection,
                            selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                case UriType.InternalPart.PART_WITH_ID:
                    String partId = uri.getLastPathSegment();
                    selection = getSelectionWithPartId(selection);
                    selectionArgs = getSelectionArgsWithPartId(selectionArgs, partId);
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_PART, projection, selection, selectionArgs, null, null,
                            sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            Uri.withAppendedPath(MmsPartLog.CONTENT_URI, partId));
                    return cursor;

                case UriType.InternalPart.PART:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_PART, projection, selection, selectionArgs, null, null,
                            sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            MmsPartLog.CONTENT_URI);
                    return cursor;

                case UriType.Part.PART_WITH_ID:
                    partId = uri.getLastPathSegment();
                    selection = getSelectionWithPartId(selection);
                    selectionArgs = getSelectionArgsWithPartId(selectionArgs, partId);
                    //$FALL-THROUGH$
                case UriType.Part.PART:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_PART,
                            restrictPartsProjectionToExternallyDefinedColumns(projection),
                            selection, selectionArgs, null, null, sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;

                default:
                    throw new IllegalArgumentException("Unsupported URI " + uri + "!");
            }
        } catch (RuntimeException e) {
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalXms.XMS:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(TABLE_XMS, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(XmsMessageLog.CONTENT_URI, null);
                }
                return count;

            case UriType.InternalPart.PART_WITH_ID:
                //$FALL-THROUGH$
            case UriType.InternalPart.PART:
                //$FALL-THROUGH$
            case UriType.Xms.XMS:
                //$FALL-THROUGH$
            case UriType.Part.PART:
                //$FALL-THROUGH$
            case UriType.Part.PART_WITH_ID:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalXms.XMS:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                String xmsId = initialValues.getAsString(XmsData.KEY_MESSAGE_ID);
                initialValues.put(XmsData.KEY_BASECOLUMN_ID, HistoryMemberBaseIdCreator
                        .createUniqueId(getContext(), XmsData.HISTORYLOG_MEMBER_ID));
                if (db.insert(TABLE_XMS, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri + '!');
                }
                Uri notificationUri = Uri.withAppendedPath(XmsMessageLog.CONTENT_URI, xmsId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.InternalPart.PART_WITH_ID:
                /* Intentional fall through */
            case UriType.InternalPart.PART:
                db = mOpenHelper.getWritableDatabase();
                long partId = db.insert(TABLE_PART, null, initialValues);
                if (INVALID_ROW_ID == partId) {
                    throw new ServerApiPersistentStorageException("Unable to insert row for URI "
                            + uri + '!');
                }
                notificationUri = ContentUris.withAppendedId(MmsPartLog.CONTENT_URI, partId);
                getContext().getContentResolver().notifyChange(notificationUri, null);
                return notificationUri;

            case UriType.Xms.XMS:
                //$FALL-THROUGH$
            case UriType.Part.PART:
                //$FALL-THROUGH$
            case UriType.Part.PART_WITH_ID:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalXms.XMS:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(TABLE_XMS, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(XmsMessageLog.CONTENT_URI, null);
                }
                return count;

            case UriType.InternalPart.PART_WITH_ID:
                String partId = uri.getLastPathSegment();
                selection = getSelectionWithPartId(selection);
                selectionArgs = getSelectionArgsWithPartId(selectionArgs, partId);
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(TABLE_PART, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(MmsPartLog.CONTENT_URI, partId), null);
                }
                return count;

            case UriType.InternalPart.PART:
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(TABLE_PART, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(MmsPartLog.CONTENT_URI, null);
                }
                return count;

            case UriType.Xms.XMS:
                //$FALL-THROUGH$
            case UriType.Part.PART:
                //$FALL-THROUGH$
            case UriType.Part.PART_WITH_ID:
                throw new UnsupportedOperationException("This provider (URI=" + uri
                        + ") supports read only access!");

            default:
                throw new IllegalArgumentException("Unsupported URI " + uri + "!");
        }
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        return openFileHelper(uri, mode);
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

    private static final class UriType {

        private static final class Xms {
            private static final int XMS = 1;
        }

        private static final class Part {
            private static final int PART = 2;
            private static final int PART_WITH_ID = 3;
        }

        private static final class InternalXms {
            private static final int XMS = 4;
        }

        private static final class InternalPart {
            private static final int PART = 5;
            private static final int PART_WITH_ID = 6;
        }
    }

    private static final class CursorType {

        private static final class Xms {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/xms";
        }

        private static final class Part {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/part";
            private static final String TYPE_ITEM = "vnd.android.cursor.item/part";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 3;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_XMS + '('
                    + XmsData.KEY_MESSAGE_ID + " TEXT NOT NULL,"
                    + XmsData.KEY_BASECOLUMN_ID + " INTEGER NOT NULL,"
                    + XmsData.KEY_CONTACT + " TEXT NOT NULL,"
                    + XmsData.KEY_CHAT_ID + " TEXT NOT NULL,"
                    + XmsData.KEY_CONTENT + " TEXT,"
                    + XmsData.KEY_MIME_TYPE + " INTEGER NOT NULL,"
                    + XmsData.KEY_DIRECTION + " INTEGER NOT NULL,"
                    + XmsData.KEY_TIMESTAMP + " INTEGER NOT NULL,"
                    + XmsData.KEY_TIMESTAMP_SENT + " INTEGER NOT NULL,"
                    + XmsData.KEY_TIMESTAMP_DELIVERED + " INTEGER NOT NULL,"
                    + XmsData.KEY_STATE + " INTEGER NOT NULL,"
                    + XmsData.KEY_REASON_CODE + " INTEGER NOT NULL,"
                    + XmsData.KEY_READ_STATUS + " INTEGER NOT NULL,"
                    + XmsData.KEY_NATIVE_ID + " INTEGER,"
                    + XmsData.KEY_NATIVE_THREAD_ID + " INTEGER,"
                    + XmsData.KEY_MESSAGE_CORRELATOR + " TEXT)" );

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_MESSAGE_ID + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_MESSAGE_ID + ')');

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_CONTACT + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_CONTACT + ')');

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_NATIVE_ID + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_NATIVE_ID + ')');

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_NATIVE_THREAD_ID + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_NATIVE_THREAD_ID + ')');

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_MESSAGE_CORRELATOR + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_MESSAGE_CORRELATOR + ')');

            // @formatter:on

            // TODO add index on mimetype

            // define another index

            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PART + '('
                    + PartData.KEY_PART_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
                    + PartData.KEY_MESSAGE_ID + " TEXT NOT NULL,"
                    + PartData.KEY_MIME_TYPE + " TEXT NOT NULL,"
                    + PartData.KEY_FILENAME + " TEXT,"
                    + PartData.KEY_FILESIZE + " INTEGER,"
                    + PartData.KEY_CONTENT + " TEXT NOT NULL,"
                    + PartData.KEY_FILEICON + " BYTES BLOB)");

            db.execSQL("CREATE INDEX " + TABLE_PART + '_' + PartData.KEY_MESSAGE_ID + "_idx" +
                    " ON " + TABLE_PART + '(' + PartData.KEY_MESSAGE_ID + ')');
            // @formatter:on
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE_XMS));
            db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE_PART));
            onCreate(db);
        }

    }
}
