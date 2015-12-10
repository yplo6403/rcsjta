
package com.gsma.rcs.cms.utils;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.MimeManager;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

public class MmsUtils {

    public static final String MMS_DIRECTORY_PATH = Environment.getExternalStorageDirectory()
            + "/rcs/mms";

    public static final List<String> sContentTypeImage = Arrays.asList(
            Constants.CONTENT_TYPE_IMAGE_JPG, Constants.CONTENT_TYPE_IMAGE_JPEG,
            Constants.CONTENT_TYPE_IMAGE_PNG, Constants.CONTENT_TYPE_IMAGE_GIF,
            Constants.CONTENT_TYPE_IMAGE_BMP);

    public static byte[] getContent(ContentResolver contentResolver, Uri uri) {

        BufferedInputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            is = new BufferedInputStream(contentResolver.openInputStream(uri));
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            CloseableUtils.tryToClose(is);
        }
    }

    public static Uri saveContent(String contentType, String contentId, byte[] data) {

        String fileName = MMS_DIRECTORY_PATH + File.separator + contentId + "_"
                + DateUtils.getMmsFileDate(System.currentTimeMillis()) + "."
                + MimeManager.getInstance().getExtensionFromMimeType(contentType);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            File file = new File(fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos, 8 * 1024);
            bos.write(data);
            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            CloseableUtils.tryToClose(bos);
            CloseableUtils.tryToClose(fos);
        }
    }
}
