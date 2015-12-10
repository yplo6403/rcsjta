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

package com.gsma.rcs.utils;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * File utilities
 * 
 * @author YPLO6403
 */
public class FileUtils {

    private static final String[] PROJECTION_DATA = {
        MediaStore.MediaColumns.DATA
    };

    private static final String SELECTION_ID = BaseColumns._ID + "=?";

    private static final Logger sLogger = Logger.getLogger(FileUtils.class.getSimpleName());

    /**
     * Copy a file to a directory
     * 
     * @param srcFile the source file (may not be null)
     * @param destDir the destination directory (may not be null)
     * @param preserveFileDate whether to preserve the file date
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate)
            throws IOException, IllegalArgumentException {
        if (srcFile == null) {
            throw new IllegalArgumentException("Source is null");
        }
        if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' is a directory");
        }
        if (destDir == null) {
            throw new IllegalArgumentException("Destination is null");
        }
        if (!destDir.exists()) {
            // Create directory if it does not exist
            if (!destDir.mkdir()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        } else {
            if (!destDir.isDirectory()) {
                throw new IllegalArgumentException("Destination '" + destDir
                        + "' is not a directory");
            }
        }
        File destFile = new File(destDir, srcFile.getName());
        if (destFile.exists() && !destFile.canWrite()) {
            throw new IOException("Destination '" + destFile + "' file exists but is read-only");
        }
        FileInputStream input = new FileInputStream(srcFile);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } finally {
            CloseableUtils.tryToClose(input);
            CloseableUtils.tryToClose(output);
        }
        // check if full content is copied
        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy from '" + srcFile + "' to '" + destFile + "'");
        }
        // preserve the file date
        if (preserveFileDate)
            destFile.setLastModified(srcFile.lastModified());
    }

    /**
     * get the oldest file from the list
     * 
     * @param files list of files
     * @return the oldest one or null
     */
    public static File getOldestFile(final File[] files) {
        if (files == null || files.length == 0) {
            return null;
        }
        File result = null;
        for (File file : files) {
            if (result == null) {
                result = file;
            } else {
                if (file.lastModified() < result.lastModified()) {
                    result = file;
                }
            }
        }
        return result;
    }

    /**
     * Delete a directory recursively
     * 
     * @param dir the directory
     * @throws IOException
     */
    public static void deleteDirectory(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getPath() + " should always be a directory!");
        }
        String[] children = dir.list();
        for (String childname : children) {
            File child = new File(dir, childname);
            if (child.isDirectory()) {
                deleteDirectory(child);
                if (!child.delete()) {
                    throw new IOException("Failed to delete file : " + child.getPath());
                }
            } else {
                if (!child.delete()) {
                    throw new IOException("Failed to delete file : " + child.getPath());
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("Failed to delete directory : " + dir.getPath());
        }
    }

    /**
     * Fetch the file name from URI
     * 
     * @param ctx Context
     * @param file URI
     * @return fileName String
     */
    public static String getFileName(Context ctx, Uri file) {
        String scheme = file.getScheme();
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(file, null, null, null, null);
            switch (scheme) {
                case ContentResolver.SCHEME_CONTENT:
                    if (cursor != null && cursor.moveToFirst()) {
                        return cursor.getString(cursor
                                .getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                    throw new IllegalArgumentException("Error in retrieving file name from the URI");

                case ContentResolver.SCHEME_FILE:
                    return file.getLastPathSegment();

                default:
                    throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Fetch the file size from URI
     * 
     * @param ctx Context
     * @param file URI
     * @return fileSize long
     */
    public static long getFileSize(Context ctx, Uri file) {
        String scheme = file.getScheme();
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(file, null, null, null, null);
            switch (scheme) {
                case ContentResolver.SCHEME_CONTENT:
                    if (cursor != null && cursor.moveToFirst()) {
                        return Long.valueOf(cursor.getString(cursor
                                .getColumnIndexOrThrow(OpenableColumns.SIZE)));
                    }
                    throw new IllegalArgumentException("Error in retrieving file size form the URI");

                case ContentResolver.SCHEME_FILE:
                    return (new File(file.getPath())).length();

                default:
                    throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Test if the stack can read data from this Uri.
     *
     * @param ctx The context
     * @param file The file URI
     * @return True if URI is readable
     */
    public static boolean isReadFromUriPossible(Context ctx, Uri file) {
        String scheme = file.getScheme();
        switch (scheme) {
            case ContentResolver.SCHEME_CONTENT:
                InputStream stream = null;
                try {
                    if (PackageManager.PERMISSION_GRANTED == ctx
                            .checkUriPermission(file, Process.myPid(), Process.myUid(),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION)) {
                        return true;
                    }
                    stream = ctx.getContentResolver().openInputStream(file);
                    stream.read();
                    return true;

                } catch (SecurityException e) {
                    sLogger.error("Failed to read from uri :" + file, e);
                    return false;

                } catch (IOException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to read from uri :" + file + ", Message="
                                + e.getMessage());
                    }
                    return false;

                } finally {
                    CloseableUtils.tryToClose(stream);
                }
            case ContentResolver.SCHEME_FILE:
                String path = file.getPath();
                if (path == null) {
                    sLogger.error("Failed to read from uri :".concat(file.toString()));
                    return false;
                }
                try {
                    return new File(path).canRead();

                } catch (SecurityException e) {
                    sLogger.error("Failed to read from uri :" + file, e);
                    return false;
                }

            default:
                throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
        }
    }

    /**
     * Get path from Uri
     * 
     * @param context The context
     * @param uri the Uri
     * @return the file path
     */
    public static String getPath(final Context context, final Uri uri) throws FileAccessException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    String docId = DocumentsContract.getDocumentId(uri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                    String id = DocumentsContract.getDocumentId(uri);
                    Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);

                } else if (isMediaDocument(uri)) { // MediaProvider
                    String docId = DocumentsContract.getDocumentId(uri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }

                    String[] selectionArgs = new String[] {
                        split[1]
                    };
                    return getDataColumn(context, contentUri, SELECTION_ID, selectionArgs);
                }
            }
        }
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            /* MediaStore (and general) */
            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }
            return getDataColumn(context, uri, null, null);

        } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            /* File */
            return uri.getPath();
        }
        throw new FileAccessException("Cannot resolve Uri '" + uri + "'!");
    }

    /**
     * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other
     * file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, PROJECTION_DATA, selection,
                    selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
            }
            return null;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is <span class="IL_AD" id="IL_AD3">Google</span> Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
