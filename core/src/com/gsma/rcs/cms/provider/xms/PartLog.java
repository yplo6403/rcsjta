
package com.gsma.rcs.cms.provider.xms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.provider.xms.model.MmsPart;
import com.gsma.rcs.cms.utils.MmsUtils;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PartLog {

    /**
     * Current instance
     */
    private static volatile PartLog sInstance;

    protected final Context mContext;
    protected final LocalContentResolver mLocalContentResolver;

    private static final String[] PROJECTION_DATA = new String[] {
            PartData.KEY_DATA
    };

    private static final String[] PROJECTION_TEXT = new String[] {
            PartData.KEY_TEXT
    };

    private static final String[] PROJECTION_WITHOUT_THUMB = new String[] {
            BaseColumns._ID,
            PartData.KEY_NATIVE_ID,
            PartData.KEY_CONTENT_TYPE,
            PartData.KEY_CONTENT_ID,
            PartData.KEY_DATA,
            PartData.KEY_TEXT
    };

    private static final String SELECTION_CONTENT_TYPE_TEXT = new StringBuilder(PartData.KEY_CONTENT_TYPE).append("='").append(Constants.CONTENT_TYPE_TEXT).append("'").toString();
    private static final String SELECTION_MMS_ID = new StringBuilder(PartData.KEY_MESSAGE_ID).append("=?").toString();
    private static final String SELECTION_DATA_NOT_NULL = new StringBuilder(PartData.KEY_DATA).append(" is not null").toString();
    private static final String SELECTION_NATIVE_ID_NOT_NULL = new StringBuilder(PartData.KEY_NATIVE_ID).append(" is not null").append(" AND ").append(SELECTION_DATA_NOT_NULL).toString();

    private static final String SELECTION_MMS_ID_CT_TEXT = new StringBuilder(SELECTION_CONTENT_TYPE_TEXT).append(" AND ").append(SELECTION_MMS_ID).toString();
    private static final String SELECTION_MMS_ID_NATIVE_ID_NOT_NULL = new StringBuilder(SELECTION_MMS_ID).append(" AND ").append(SELECTION_NATIVE_ID_NOT_NULL).append(" AND ").append(SELECTION_DATA_NOT_NULL).toString();

    private static final Logger sLogger = Logger.getLogger(PartLog.class.getSimpleName());

    /**
     * Gets the instance of PartLog singleton
     * @param context
     * @return the instance of PartLog singleton
     */
    public static PartLog getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (PartLog.class) {
            if (sInstance == null) {
                sInstance = new PartLog(context);
            }
        }
        return sInstance;
    }

    /**
     * Constructor
     *
     * @param context
     */
    private PartLog(Context context) {
        mContext = context;
        mLocalContentResolver = new LocalContentResolver(context.getContentResolver());
    }

    /**
     * Add Sms
     * @param mmsParts
     * @return baseColumnId
     */
    public void addParts(String mmsId, List<MmsPart> mmsParts) {

        for(MmsPart mmsPart : mmsParts){
            ContentValues partValues = new ContentValues();
            String nativeId = mmsPart.getNativeId();
            partValues.put(PartData.KEY_NATIVE_ID, nativeId);
            partValues.put(PartData.KEY_MESSAGE_ID, mmsId);
            partValues.put(PartData.KEY_CONTENT_ID, mmsPart.getContentId());
            partValues.put(PartData.KEY_DATA, mmsPart.getPath());
            partValues.put(PartData.KEY_TEXT, mmsPart.getText());
            String contentType = mmsPart.getContentType();
            partValues.put(PartData.KEY_CONTENT_TYPE, contentType);
            if(MmsUtils.CONTENT_TYPE_IMAGE.contains(contentType)){
                byte[] thumb = MmsUtils.createThumb(mmsPart);
                partValues.put(PartData.KEY_THUMB, thumb);
            }
            mLocalContentResolver.insert(PartData.CONTENT_URI, partValues);
        }
    }

    public List<MmsPart> getParts(String mmsId, boolean withThumb) {
        return (withThumb ? getParts(mmsId) : getPartsWithoutThumb(mmsId));
    }

    private  List<MmsPart>  getPartsWithoutThumb(String mmsId) {
        List<MmsPart> parts = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(PartData.CONTENT_URI, PROJECTION_WITHOUT_THUMB, SELECTION_MMS_ID, new String[]{mmsId}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            int nativeIdIdx = cursor.getColumnIndexOrThrow(PartData.KEY_NATIVE_ID);
            int dataIdx = cursor.getColumnIndexOrThrow(PartData.KEY_DATA);
            int textIdx = cursor.getColumnIndexOrThrow(PartData.KEY_TEXT);
            int contentTypeIdx = cursor.getColumnIndexOrThrow(PartData.KEY_CONTENT_TYPE);
            int contentIdIdx = cursor.getColumnIndexOrThrow(PartData.KEY_CONTENT_ID);
            while(cursor.moveToNext()){
                parts.add(new MmsPart(
                        cursor.getString(idIdx),
                        cursor.getString(nativeIdIdx),
                        cursor.getString(contentTypeIdx),
                        cursor.getString(contentIdIdx),
                        cursor.getString(dataIdx),
                        cursor.getString(textIdx)));
            }
            return parts;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private  List<MmsPart>  getParts(String mmsId) {
        List<MmsPart> parts = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(PartData.CONTENT_URI, null, SELECTION_MMS_ID, new String[]{mmsId}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int _idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            int nativePartIdIdx = cursor.getColumnIndexOrThrow(PartData.KEY_NATIVE_ID);
            int dataIdx = cursor.getColumnIndexOrThrow(PartData.KEY_DATA);
            int textIdx = cursor.getColumnIndexOrThrow(PartData.KEY_TEXT);
            int contentTypeIdx = cursor.getColumnIndexOrThrow(PartData.KEY_CONTENT_TYPE);
            int contentIdIdx = cursor.getColumnIndexOrThrow(PartData.KEY_CONTENT_ID);
            int thumbIdx = cursor.getColumnIndexOrThrow(PartData.KEY_THUMB);
            while(cursor.moveToNext()){
                parts.add(new MmsPart(
                        cursor.getString(_idIdx),
                        cursor.getString(nativePartIdIdx),
                        cursor.getString(contentTypeIdx),
                        cursor.getString(contentIdIdx),
                        cursor.getString(dataIdx),
                        cursor.getString(textIdx),
                        cursor.getBlob(thumbIdx)));
            }
            return parts;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private  List<String>  getDataPaths(String mmsId) {
        List<String> dataPaths = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(PartData.CONTENT_URI, PROJECTION_DATA, SELECTION_MMS_ID_NATIVE_ID_NOT_NULL, new String[]{mmsId}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int dataIdx = cursor.getColumnIndexOrThrow(PartData.KEY_DATA);
            while(cursor.moveToNext()){
                dataPaths.add(cursor.getString(dataIdx));
            }
            return dataPaths;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private  List<String>  getDataPaths() {
        List<String> dataPaths = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(PartData.CONTENT_URI, PROJECTION_DATA, SELECTION_NATIVE_ID_NOT_NULL, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            int dataIdx = cursor.getColumnIndexOrThrow(PartData.KEY_DATA);
            while(cursor.moveToNext()){
                dataPaths.add(cursor.getString(dataIdx));
            }
            return dataPaths;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    public String getTextContent(String mmsId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(PartData.CONTENT_URI, PROJECTION_TEXT, SELECTION_MMS_ID_CT_TEXT, new String[]{mmsId}, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsLogData.CONTENT_URI);
            if(cursor.moveToNext()){
                return cursor.getString(cursor.getColumnIndexOrThrow(PartData.KEY_TEXT));
            }
            return null;
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * @param mmsId
     */
    public void deleteParts(String mmsId){
        List<String> dataToDelete = getDataPaths(mmsId);
        mLocalContentResolver.delete(PartData.CONTENT_URI, SELECTION_MMS_ID, new String[]{mmsId});
        for(String data : dataToDelete){
            MmsUtils.tryToDelete(data);
        }
    }

    /**
     */
    public void deleteAll() {
        List<String> dataToDelete = getDataPaths();
        mLocalContentResolver.delete(PartData.CONTENT_URI, null,null);
        for(String data : dataToDelete){
            MmsUtils.tryToDelete(data);
        }
    }

}
