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

package com.orangelabs.rcs.ri.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    private static MimeTypeMap sMimeTypeMapSingleton;

    private static final int THUMB_SIZE = 100;

    /**
     * Fetch the file name from URI
     *
     * @param context Context
     * @param file URI
     * @return fileName String
     * @throws IllegalArgumentException
     */
    public static String getFileName(Context context, Uri file) throws IllegalArgumentException {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(file, null, null, null, null);
            if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(cursor
                            .getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                } else {
                    throw new IllegalArgumentException("Error in retrieving file name from the URI");
                }
            } else if (ContentResolver.SCHEME_FILE.equals(file.getScheme())) {
                return file.getLastPathSegment();
            }
            throw new IllegalArgumentException("Unsupported URI scheme");

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Fetch the file size from URI
     *
     * @param context Context
     * @param file URI
     * @return fileSize long
     * @throws IllegalArgumentException
     */
    public static long getFileSize(Context context, Uri file) throws IllegalArgumentException {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(file, null, null, null, null);
            if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
                if (cursor != null && cursor.moveToFirst()) {
                    return Long.valueOf(cursor.getString(cursor
                            .getColumnIndexOrThrow(OpenableColumns.SIZE)));
                } else {
                    throw new IllegalArgumentException("Error in retrieving file size form the URI");
                }
            } else if (ContentResolver.SCHEME_FILE.equals(file.getScheme())) {
                return (new File(file.getPath())).length();
            }
            throw new IllegalArgumentException("Unsupported URI scheme");

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Returns URL extension
     *
     * @param filename The filename
     * @return Extension
     */
    private static String getFileExtension(String filename) {
        if (filename.indexOf('.') != -1) {
            return filename.substring(filename.lastIndexOf('.') + 1);
        }
        return null;
    }

    /**
     * Returns mime type from filename
     *
     * @param filename The filename
     * @return Extension
     */
    public static String getMimeType(String filename) {
        String extension = getFileExtension(filename);
        if (extension == null) {
            return null;
        }
        if (sMimeTypeMapSingleton == null) {
            sMimeTypeMapSingleton = MimeTypeMap.getSingleton();
        }
        return sMimeTypeMapSingleton.getMimeTypeFromExtension(extension);
    }

    /**
     * Is a image type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isImageType(String mime) {
        return mime.toLowerCase().startsWith("image/");
    }

    public static byte[] createThumb(ContentResolver contentResolver, Uri uri) throws IOException {
        InputStream is = null;
        try {
            is = contentResolver.openInputStream(uri);
            Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(is),
                    THUMB_SIZE, THUMB_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Is a video type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isVideoType(String mime) {
        return mime.toLowerCase().startsWith("video/");
    }

    /**
     * Open file
     *
     * @param activity the activity
     * @param mimeType the mime type
     * @param action the action
     */
    public static void openFile(Activity activity, String mimeType, int action) {
        Intent intent = forgeIntentToOpenFile();
        intent.setType(mimeType);
        activity.startActivityForResult(intent, action);
    }

    private static Intent forgeIntentToOpenFile() {
        Intent intent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }

    public static void openFiles(Activity activity, String[] mimeTypes, int action) {
        Intent intent = forgeIntentToOpenFile();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        } else {
            intent.setType(mimeTypes[0]);
        }
        activity.startActivityForResult(intent, action);
    }

    /**
     * Saves the read/write permission for later use by the stack.
     *
     * @param file Uri of file to transfer
     */
    public static void takePersistableContentUriPermission(Context context, Uri file) {
        if (!(ContentResolver.SCHEME_CONTENT.equals(file.getScheme()))) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContentResolver contentResolver = context.getContentResolver();
            contentResolver.takePersistableUriPermission(file,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
    }

    /**
     * Converts byte size into human readable format
     *
     * @param bytes number of bytes to display
     * @param si True is binary units or SI units else.
     * @return String
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
