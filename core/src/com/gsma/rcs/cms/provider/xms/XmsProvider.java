
package com.gsma.rcs.cms.provider.xms;

import com.gsma.rcs.provider.history.HistoryMemberBaseIdCreator;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.DatabaseUtils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.io.FileNotFoundException;

public class XmsProvider extends ContentProvider {

    /* package private */static final int INVALID_ROW_ID = -1;

    private static final String SELECTION_WITH_ID_ONLY = BaseColumns._ID.concat("=?");

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(XmsLogData.CONTENT_URI.getAuthority(),
                XmsLogData.CONTENT_URI.getPath().substring(1), UriType.Xms.XMS);
        sUriMatcher.addURI(XmsLogData.CONTENT_URI.getAuthority(),
                XmsLogData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.Xms.XMS_WITH_ID);
        sUriMatcher.addURI(PartData.CONTENT_URI.getAuthority(),
                PartData.CONTENT_URI.getPath().substring(1), UriType.Part.PART);
        sUriMatcher.addURI(PartData.CONTENT_URI.getAuthority(),
                PartData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.Part.PART_WITH_ID);
    }

    private static final class UriType {

        private static final class Xms {
            private static final int XMS_WITH_ID = 1;
            private static final int XMS = 2;
        }

        private static final class Part {
            private static final int PART_WITH_ID = 3;
            private static final int PART = 4;
        }
    }

    private static final class CursorType {

        private static final class Xms {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.rcs.cms.xms";
            private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.rcs.cms.xms";
        }

        private static final class Part {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.rcs.cms.xms";
            private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.rcs.cms.xms";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context ctx) {
            super(ctx, XmsLogData.DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(XmsLogData.TABLE_XMS)
                    .append('(').append(XmsLogData.KEY_BASECOLUMN_ID).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(XmsLogData.KEY_NATIVE_PROVIDER_ID).append(" TEXT,")
                    .append(XmsLogData.KEY_NATIVE_THREAD_ID).append(" TEXT,")
                    .append(XmsLogData.KEY_CONTACT).append(" TEXT NOT NULL,")
                    .append(XmsLogData.KEY_SUBJECT).append(" TEXT,")
                    .append(XmsLogData.KEY_CONTENT).append(" TEXT,")
                    .append(XmsLogData.KEY_DATE).append(" INTEGER NOT NULL,")
                    .append(XmsLogData.KEY_DIRECTION).append(" INTEGER NOT NULL,")
                    .append(XmsLogData.KEY_MIME_TYPE).append(" INTEGER NOT NULL,")
                    .append(XmsLogData.KEY_READ_STATUS).append(" INTEGER NOT NULL,")
                    .append(XmsLogData.KEY_DELETE_STATUS).append(" INTEGER NOT NULL,")
                    .append(XmsLogData.KEY_PUSH_STATUS).append(" INTEGER NOT NULL,")
                    .append(XmsLogData.KEY_DELIVERY_DATE).append(" INTEGER NOT NULL,")
                    .append(XmsLogData.KEY_MESSAGE_CORRELATOR).append(" TEXT,")
                    .append(XmsLogData.KEY_MMS_ID).append(" TEXT,")
                    .append(XmsLogData.KEY_ATTACHMENT).append(" TEXT)").toString());
            
            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsLogData.TABLE_XMS).append('_')
                    .append(XmsLogData.KEY_BASECOLUMN_ID).append("_idx").append(" ON ").append(XmsLogData.TABLE_XMS)
                    .append('(').append(XmsLogData.KEY_BASECOLUMN_ID).append(')').toString());

            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsLogData.TABLE_XMS).append('_')
                    .append(XmsLogData.KEY_CONTACT).append("_idx").append(" ON ").append(XmsLogData.TABLE_XMS)
                    .append('(').append(XmsLogData.KEY_CONTACT).append(')').toString());

            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsLogData.TABLE_XMS).append('_')
                    .append(XmsLogData.KEY_NATIVE_PROVIDER_ID).append("_idx").append(" ON ").append(XmsLogData.TABLE_XMS)
                    .append('(').append(XmsLogData.KEY_NATIVE_PROVIDER_ID).append(')').toString());

            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsLogData.TABLE_XMS).append('_')
                    .append(XmsLogData.KEY_MESSAGE_CORRELATOR).append("_idx").append(" ON ").append(XmsLogData.TABLE_XMS)
                    .append('(').append(XmsLogData.KEY_MESSAGE_CORRELATOR).append(')').toString());

            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsLogData.TABLE_XMS).append('_')
                    .append(XmsLogData.KEY_MMS_ID).append("_idx").append(" ON ").append(XmsLogData.TABLE_XMS)
                    .append('(').append(XmsLogData.KEY_MMS_ID).append(')').toString());

            // TODO add index on mimetype

            // define another index

            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(PartData.TABLE_PART)
                    .append('(').append(PartData.KEY_BASECOLUMN_ID).append(" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,")
                    .append(PartData.KEY_NATIVE_ID).append(" TEXT,")
                    .append(PartData.KEY_MESSAGE_ID).append(" TEXT NOT NULL,")
                    .append(PartData.KEY_CONTENT_TYPE).append(" TEXT NOT NULL,")
                    .append(PartData.KEY_CONTENT_ID).append(" TEXT,")
                    .append(PartData.KEY_DATA).append(" TEXT,")
                    .append(PartData.KEY_THUMB).append(" BYTES BLOB,")
                    .append(PartData.KEY_TEXT).append(" TEXT)").toString());

            db.execSQL(new StringBuilder("CREATE INDEX ").append(PartData.TABLE_PART).append('_')
                    .append(PartData.KEY_MESSAGE_ID).append("_idx").append(" ON ").append(PartData.TABLE_PART)
                    .append('(').append(PartData.KEY_MESSAGE_ID).append(')').toString());

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(XmsLogData.TABLE_XMS));
            db.execSQL("DROP TABLE IF EXISTS ".concat(PartData.TABLE_PART));
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
            case UriType.Xms.XMS:
                return CursorType.Xms.TYPE_DIRECTORY;
            case UriType.Xms.XMS_WITH_ID:
                return CursorType.Xms.TYPE_ITEM;
            case UriType.Part.PART:
                return CursorType.Part.TYPE_DIRECTORY;
            case UriType.Part.PART_WITH_ID:
                return CursorType.Part.TYPE_ITEM;
            default:
                throw new IllegalArgumentException(
                        new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
        }
    }

    private String getSelectionWithId(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return SELECTION_WITH_ID_ONLY;
        }
        return new StringBuilder("(").append(SELECTION_WITH_ID_ONLY).append(") AND (")
                .append(selection).append(')').toString();
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
            case UriType.Xms.XMS_WITH_ID:
                String id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Xms.XMS:
                SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                cursor = db.query(XmsLogData.TABLE_XMS, projection, selection, selectionArgs, null, null,
                        sort);
                return cursor;
            case UriType.Part.PART_WITH_ID:
                id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Part.PART:
                db = mOpenHelper.getReadableDatabase();
                cursor = db.query(PartData.TABLE_PART, projection, selection, selectionArgs, null, null,
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
            case UriType.Xms.XMS_WITH_ID:
                String id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Xms.XMS:
                SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                return db.update(XmsLogData.TABLE_XMS, values, selection, selectionArgs);

            case UriType.Part.PART_WITH_ID:
                id = uri.getLastPathSegment();
                selection = getSelectionWithId(selection);
                selectionArgs = getSelectionArgsWithId(selectionArgs, id);
                /* Intentional fall through */
            case UriType.Part.PART:
                db = mOpenHelper.getReadableDatabase();
                return db.update(PartData.TABLE_PART, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException(
                        new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Xms.XMS_WITH_ID:
                /* Intentional fall through */
            case UriType.Xms.XMS:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                long uniqueId = HistoryMemberBaseIdCreator
                        .createUniqueId(getContext(), XmsLogData.HISTORYLOG_MEMBER_ID);
                initialValues.put(XmsLogData.KEY_BASECOLUMN_ID, uniqueId);
                if (db.insert(XmsLogData.TABLE_XMS, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException(
                            new StringBuilder("Unable to insert row for URI ").append(uri)
                                    .append('!').toString());
                }
                return Uri.withAppendedPath(uri, String.valueOf(uniqueId));
            case UriType.Part.PART_WITH_ID:
                /* Intentional fall through */
            case UriType.Part.PART:
                db = mOpenHelper.getWritableDatabase();
                long id = db.insert(PartData.TABLE_PART, null, initialValues);
                if (INVALID_ROW_ID == id) {
                    throw new ServerApiPersistentStorageException(
                            new StringBuilder("Unable to insert row for URI ").append(uri)
                                    .append('!').toString());
                }
                return Uri.withAppendedPath(uri, String.valueOf(id));
            default:
                throw new IllegalArgumentException(
                        new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        switch (sUriMatcher.match(uri)) {
            case UriType.Xms.XMS_WITH_ID:
                /* Intentional fall through */
            case UriType.Xms.XMS:
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int nb = db.delete(XmsLogData.TABLE_XMS, where, whereArgs);
                if (nb == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException(
                            new StringBuilder("Unable to insert row for URI ").append(uri)
                                    .append('!').toString());
                }
                return nb;
            case UriType.Part.PART_WITH_ID:
                /* Intentional fall through */
            case UriType.Part.PART:
                db = mOpenHelper.getWritableDatabase();
                nb = db.delete(PartData.TABLE_PART, where, whereArgs);
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

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        return openFileHelper(uri, mode);
    }
}
