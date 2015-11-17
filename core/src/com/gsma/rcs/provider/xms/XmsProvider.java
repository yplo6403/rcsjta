
package com.gsma.rcs.provider.xms;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.history.HistoryMemberBaseIdCreator;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.DatabaseUtils;
import com.gsma.services.rcs.cms.MmsPartLog;
import com.gsma.services.rcs.cms.XmsMessageLog;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    private static final String SELECTION_WITH_XMS_ID_ONLY = XmsData.KEY_MESSAGE_ID.concat("=?");

    private static final String SELECTION_WITH_PART_ID_ONLY = PartData.KEY_PART_ID.concat("=?");

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(XmsMessageLog.CONTENT_URI.getAuthority(), XmsMessageLog.CONTENT_URI
                .getPath().substring(1), UriType.Xms.XMS);
        sUriMatcher.addURI(XmsMessageLog.CONTENT_URI.getAuthority(), XmsMessageLog.CONTENT_URI
                .getPath().substring(1).concat("/*"), UriType.Xms.XMS_WITH_ID);

        sUriMatcher.addURI(MmsPartLog.CONTENT_URI.getAuthority(), MmsPartLog.CONTENT_URI.getPath()
                .substring(1), UriType.Part.PART);
        sUriMatcher.addURI(MmsPartLog.CONTENT_URI.getAuthority(), MmsPartLog.CONTENT_URI.getPath()
                .substring(1).concat("/*"), UriType.Part.PART_WITH_ID);

        sUriMatcher.addURI(XmsData.CONTENT_URI.getAuthority(), XmsData.CONTENT_URI.getPath()
                .substring(1), UriType.InternalXms.XMS);
        sUriMatcher.addURI(XmsData.CONTENT_URI.getAuthority(), XmsData.CONTENT_URI.getPath()
                .substring(1).concat("/*"), UriType.InternalXms.XMS_WITH_ID);

        sUriMatcher.addURI(PartData.CONTENT_URI.getAuthority(), PartData.CONTENT_URI.getPath()
                .substring(1), UriType.InternalPart.PART);
        sUriMatcher.addURI(PartData.CONTENT_URI.getAuthority(), PartData.CONTENT_URI.getPath()
                .substring(1).concat("/*"), UriType.InternalPart.PART_WITH_ID);
    }

    /**
     * Strings to allow projection for exposed URI to a set of columns.
     */
    private static final String[] XMS_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS = new String[] {
            XmsData.KEY_BASECOLUMN_ID, XmsData.KEY_MESSAGE_ID, XmsData.KEY_CONTACT,
            XmsData.KEY_BODY, XmsData.KEY_MIME_TYPE, XmsData.KEY_DIRECTION, XmsData.KEY_TIMESTAMP,
            XmsData.KEY_TIMESTAMP_SENT, XmsData.KEY_TIMESTAMP_DELIVERED, XmsData.KEY_STATE,
            XmsData.KEY_REASON_CODE, XmsData.KEY_READ_STATUS
    };

    private static final Set<String> XMS_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS = new HashSet<>(
            Arrays.asList(XMS_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS));

    private static final String[] PART_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS = new String[] {
            PartData.KEY_PART_ID, PartData.KEY_MESSAGE_ID, PartData.KEY_MIME_TYPE,
            PartData.KEY_CONTENT, PartData.KEY_FILEICON
    };

    private static final Set<String> PART_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS = new HashSet<>(
            Arrays.asList(PART_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS));

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Xms.XMS:
                /* Intentional fall through */
            case UriType.InternalXms.XMS:
                return CursorType.Xms.TYPE_DIRECTORY;

            case UriType.Xms.XMS_WITH_ID:
                /* Intentional fall through */
            case UriType.InternalXms.XMS_WITH_ID:
                return CursorType.Xms.TYPE_ITEM;

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

    private String getSelectionWithXmsId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_XMS_ID_ONLY;
        }
        return "(" + SELECTION_WITH_XMS_ID_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithXmsId(String[] selectionArgs, String xmsId) {
        String[] keySelectionArg = new String[] {
            xmsId
        };
        if (selectionArgs == null) {
            return keySelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(keySelectionArg, selectionArgs);
    }

    private String getSelectionWithPartId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_PART_ID_ONLY;
        }
        return "(" + SELECTION_WITH_PART_ID_ONLY + ") AND (" + selection + ')';
    }

    private String[] getSelectionArgsWithPartId(String[] selectionArgs, String id) {
        String[] keySelectionArg = new String[] {
            id
        };
        if (selectionArgs == null) {
            return keySelectionArg;
        }
        return DatabaseUtils.appendSelectionArgs(keySelectionArg, selectionArgs);
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

    private String[] restrictPartProjectionToExternallyDefinedColumns(String[] projection)
            throws UnsupportedOperationException {
        if (projection == null || projection.length == 0) {
            return PART_COLUMNS_ALLOWED_FOR_EXTERNAL_ACCESS;
        }
        for (String projectedColumn : projection) {
            if (!PART_COLUMNS_SET_ALLOWED_FOR_EXTERNAL_ACCESS.contains(projectedColumn)) {
                throw new UnsupportedOperationException("No visibility to the accessed column "
                        + projectedColumn + "!");
            }
        }
        return projection;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sort) {
        Cursor cursor = null;
        try {
            switch (sUriMatcher.match(uri)) {
                case UriType.InternalXms.XMS_WITH_ID:
                    String xmsId = uri.getLastPathSegment();
                    selection = getSelectionWithXmsId(selection);
                    selectionArgs = getSelectionArgsWithXmsId(selectionArgs, xmsId);
                    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_XMS, projection, selection, selectionArgs, null, null,
                            sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            Uri.withAppendedPath(XmsMessageLog.CONTENT_URI, xmsId));
                    return cursor;

                case UriType.InternalXms.XMS:
                    db = mOpenHelper.getReadableDatabase();
                    cursor = db.query(TABLE_XMS, projection, selection, selectionArgs, null, null,
                            sort);
                    CursorUtil.assertCursorIsNotNull(cursor, uri);
                    cursor.setNotificationUri(getContext().getContentResolver(),
                            XmsMessageLog.CONTENT_URI);
                    return cursor;

                case UriType.Xms.XMS_WITH_ID:
                    xmsId = uri.getLastPathSegment();
                    selection = getSelectionWithXmsId(selection);
                    selectionArgs = getSelectionArgsWithXmsId(selectionArgs, xmsId);
                    //$FALL-THROUGH$
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
                            restrictPartProjectionToExternallyDefinedColumns(projection),
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
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalXms.XMS_WITH_ID:
                String xmsId = uri.getLastPathSegment();
                selection = getSelectionWithXmsId(selection);
                selectionArgs = getSelectionArgsWithXmsId(selectionArgs, xmsId);
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.update(TABLE_XMS, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(XmsMessageLog.CONTENT_URI, xmsId), null);
                }
                return count;

            case UriType.InternalXms.XMS:
                db = mOpenHelper.getWritableDatabase();
                count = db.update(TABLE_XMS, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(XmsMessageLog.CONTENT_URI, null);
                }
                return count;

            case UriType.InternalPart.PART_WITH_ID:
                String partId = uri.getLastPathSegment();
                selection = getSelectionWithPartId(selection);
                selectionArgs = getSelectionArgsWithPartId(selectionArgs, partId);
                db = mOpenHelper.getWritableDatabase();
                count = db.update(TABLE_PART, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(MmsPartLog.CONTENT_URI, partId), null);
                }
                return count;

            case UriType.InternalPart.PART:
                db = mOpenHelper.getWritableDatabase();
                count = db.update(TABLE_PART, values, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(MmsPartLog.CONTENT_URI, null);
                }
                return count;

            case UriType.Xms.XMS:
                //$FALL-THROUGH$
            case UriType.Xms.XMS_WITH_ID:
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
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalXms.XMS_WITH_ID:
                //$FALL-THROUGH$
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
            case UriType.Xms.XMS_WITH_ID:
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
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.InternalXms.XMS_WITH_ID:
                String xmsId = uri.getLastPathSegment();
                selection = getSelectionWithXmsId(selection);
                selectionArgs = getSelectionArgsWithXmsId(selectionArgs, xmsId);
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int count = db.delete(TABLE_XMS, selection, selectionArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Uri.withAppendedPath(XmsMessageLog.CONTENT_URI, xmsId), null);
                }
                return count;

            case UriType.InternalXms.XMS:
                db = mOpenHelper.getWritableDatabase();
                count = db.delete(TABLE_XMS, selection, selectionArgs);
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
            case UriType.Xms.XMS_WITH_ID:
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
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return openFileHelper(uri, mode);
    }

    private static final class UriType {

        private static final class Xms {
            private static final int XMS = 1;
            private static final int XMS_WITH_ID = 2;
        }

        private static final class Part {
            private static final int PART = 3;
            private static final int PART_WITH_ID = 4;
        }

        private static final class InternalXms {
            private static final int XMS = 5;
            private static final int XMS_WITH_ID = 6;
        }

        private static final class InternalPart {
            private static final int PART = 7;
            private static final int PART_WITH_ID = 8;
        }
    }

    private static final class CursorType {

        private static final class Xms {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/xms";
            private static final String TYPE_ITEM = "vnd.android.cursor.item/xms";
        }

        private static final class Part {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/part";
            private static final String TYPE_ITEM = "vnd.android.cursor.item/part";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_XMS + '('
                    + XmsData.KEY_MESSAGE_ID + " TEXT NOT NULL PRIMARY KEY,"
                    + XmsData.KEY_BASECOLUMN_ID + " INTEGER NOT NULL,"
                    + XmsData.KEY_CONTACT + " TEXT NOT NULL,"
                    + XmsData.KEY_BODY + " TEXT,"
                    + XmsData.KEY_MIME_TYPE + " INTEGER NOT NULL,"
                    + XmsData.KEY_DIRECTION + " INTEGER NOT NULL,"
                    + XmsData.KEY_TIMESTAMP + " INTEGER NOT NULL,"
                    + XmsData.KEY_TIMESTAMP_SENT + " INTEGER NOT NULL,"
                    + XmsData.KEY_TIMESTAMP_DELIVERED + " INTEGER NOT NULL,"
                    + XmsData.KEY_STATE + " INTEGER NOT NULL,"
                    + XmsData.KEY_REASON_CODE + " INTEGER NOT NULL,"
                    + XmsData.KEY_READ_STATUS + " INTEGER NOT NULL,"
                    + XmsData.KEY_NATIVE_ID + " INTEGER,"
                    + XmsData.KEY_MESSAGE_CORRELATOR + " TEXT,"
                    + XmsData.KEY_MMS_ID + " TEXT)" );

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_MESSAGE_ID + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_MESSAGE_ID + ')');

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_CONTACT + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_CONTACT + ')');

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_NATIVE_ID + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_NATIVE_ID + ')');

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_MESSAGE_CORRELATOR + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_MESSAGE_CORRELATOR + ')');

            db.execSQL("CREATE INDEX " + TABLE_XMS + '_' + XmsData.KEY_MMS_ID + "_idx" +
                    " ON " + TABLE_XMS + '(' + XmsData.KEY_MMS_ID + ')');

            // @formatter:on

            // TODO add index on mimetype

            // define another index

            // @formatter:off
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PART + '('
                    + PartData.KEY_PART_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
                    + PartData.KEY_MESSAGE_ID + " TEXT NOT NULL,"
                    + PartData.KEY_MIME_TYPE + " TEXT NOT NULL,"
                    + PartData.KEY_FILENAME + " TEXT,"
                    + PartData.KEY_FILESIZE + " TEXT,"
                    + PartData.KEY_CONTENT + " BYTES BLOB,"
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
