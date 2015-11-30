package com.gsma.rcs.cms.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.MimeManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class MmsUtils {

    public static final String MMS_DIRECTORY_PATH = Environment.getExternalStorageDirectory() + "/rcs/mms";

    public static final List<String> CONTENT_TYPE_IMAGE= Arrays.asList(new String[]{
            Constants.CONTENT_TYPE_IMAGE_JPG,
            Constants.CONTENT_TYPE_IMAGE_JPEG,
            Constants.CONTENT_TYPE_IMAGE_PNG,
            Constants.CONTENT_TYPE_IMAGE_GIF,
            Constants.CONTENT_TYPE_IMAGE_BMP
    });

    private static final int THUMB_SIZE = 100;

    public static byte[] createThumb(ContentResolver contentResolver, Uri uri){

        InputStream is = null;
        try{
            is = contentResolver.openInputStream(uri);
            Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(is), THUMB_SIZE, THUMB_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();
        } catch (FileNotFoundException e) {
            // TODO do not catch
            e.printStackTrace();
            return null;
        }
        finally {
            CloseableUtils.tryToClose(is);
        }
    }

    public static byte[] createThumb(byte[] content){
        Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeByteArray(content, 0, content.length), THUMB_SIZE, THUMB_SIZE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    public static byte[] getContent(ContentResolver contentResolver, Uri uri){

        BufferedInputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try{
            is = new BufferedInputStream(contentResolver.openInputStream(uri));
            byte[] buf = new byte[1024];
            int n;
            while ((n=is.read(buf,0,buf.length))!=-1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            CloseableUtils.tryToClose(is);
        }
    }

    public static Uri saveContent(String contentType, String contentId, byte[] data){

        String fileName = new StringBuilder(MMS_DIRECTORY_PATH).append(File.separator).
                append(contentId).append("_").
                append(DateUtils.getMmsFileDate(System.currentTimeMillis())).
                append(".").append(MimeManager.getInstance().getExtensionFromMimeType(contentType)).toString();
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
        }
        finally{
            CloseableUtils.tryToClose(bos);
            CloseableUtils.tryToClose(fos);
        }
    }
}
