
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
import android.provider.BaseColumns;
import android.text.TextUtils;

public class XmsProvider extends ContentProvider {

    /* package private */static final int INVALID_ROW_ID = -1;

    private static final String SELECTION_WITH_ID_ONLY = BaseColumns._ID.concat("=?");

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(XmsData.CONTENT_URI.getAuthority(),
                XmsData.CONTENT_URI.getPath().substring(1), UriType.Xms.XMS);
        sUriMatcher.addURI(XmsData.CONTENT_URI.getAuthority(),
                XmsData.CONTENT_URI.getPath().substring(1).concat("/*"),
                UriType.Xms.XMS_WITH_ID);
    }

    private static final class UriType {

        private static final class Xms {
            private static final int XMS_WITH_ID = 1;
            private static final int XMS = 2;
        }
    }

    private static final class CursorType {

        private static final class Xms {
            private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/com.gsma.rcs.cms.xms";
            private static final String TYPE_ITEM = "vnd.android.cursor.item/com.gsma.rcs.cms.xms";
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context ctx) {
            super(ctx, XmsData.DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(XmsData.TABLE_XMS)
                    .append('(').append(XmsData.KEY_BASECOLUMN_ID).append(" TEXT NOT NULL PRIMARY KEY,")
                    .append(XmsData.KEY_NATIVE_PROVIDER_ID).append(" TEXT,")
                    .append(XmsData.KEY_CONTACT).append(" TEXT NOT NULL,")
                    .append(XmsData.KEY_SUBJECT).append(" TEXT,")
                    .append(XmsData.KEY_CONTENT).append(" TEXT,")
                    .append(XmsData.KEY_DATE).append(" INTEGER NOT NULL,")
                    .append(XmsData.KEY_DIRECTION).append(" INTEGER NOT NULL,")                    
                    .append(XmsData.KEY_MIME_TYPE).append(" INTEGER NOT NULL,")
                    .append(XmsData.KEY_READ_STATUS).append(" INTEGER NOT NULL,")
                    .append(XmsData.KEY_DELETE_STATUS).append(" INTEGER NOT NULL,")
                    .append(XmsData.KEY_PUSH_STATUS).append(" INTEGER NOT NULL,")
                    .append(XmsData.KEY_DELIVERY_DATE).append(" INTEGER NOT NULL,")                    
                    .append(XmsData.KEY_MESSAGE_CORRELATOR).append(" TEXT,")
                    .append(XmsData.KEY_ATTACHMENT).append(" TEXT)").toString());                    
            
            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsData.TABLE_XMS).append('_')
                    .append(XmsData.KEY_BASECOLUMN_ID).append("_idx").append(" ON ").append(XmsData.TABLE_XMS)
                    .append('(').append(XmsData.KEY_BASECOLUMN_ID).append(')').toString());

            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsData.TABLE_XMS).append('_')
                    .append(XmsData.KEY_CONTACT).append("_idx").append(" ON ").append(XmsData.TABLE_XMS)
                    .append('(').append(XmsData.KEY_CONTACT).append(')').toString());

            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsData.TABLE_XMS).append('_')
                    .append(XmsData.KEY_NATIVE_PROVIDER_ID).append("_idx").append(" ON ").append(XmsData.TABLE_XMS)
                    .append('(').append(XmsData.KEY_NATIVE_PROVIDER_ID).append(')').toString());

            db.execSQL(new StringBuilder("CREATE INDEX ").append(XmsData.TABLE_XMS).append('_')
                    .append(XmsData.KEY_MESSAGE_CORRELATOR).append("_idx").append(" ON ").append(XmsData.TABLE_XMS)
                    .append('(').append(XmsData.KEY_MESSAGE_CORRELATOR).append(')').toString());

            // TODO
            // define another index
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS ".concat(XmsData.TABLE_XMS));
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
                cursor = db.query(XmsData.TABLE_XMS, projection, selection, selectionArgs, null, null,
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
                return db.update(XmsData.TABLE_XMS, values, selection, selectionArgs);
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
                        .createUniqueId(getContext(), XmsData.HISTORYLOG_MEMBER_ID);
                System.out.println("unique Id :" + uniqueId);
                initialValues.put(XmsData.KEY_BASECOLUMN_ID, uniqueId);
                if (db.insert(XmsData.TABLE_XMS, null, initialValues) == INVALID_ROW_ID) {
                    throw new ServerApiPersistentStorageException(
                            new StringBuilder("Unable to insert row for URI ").append(uri)
                                    .append('!').toString());
                }
                return Uri.withAppendedPath(uri, String.valueOf(uniqueId));
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
                int nb = db.delete(XmsData.TABLE_XMS, where, whereArgs);
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
