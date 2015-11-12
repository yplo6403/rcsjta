package com.gsma.rcs.cms.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;

import com.gsma.rcs.cms.CmsService;
import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.observer.XmsObserverUtils;
import com.gsma.rcs.cms.provider.xms.PartData;
import com.gsma.rcs.cms.provider.xms.model.MmsPart;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.MimeManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
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

    public static byte[] createThumb(MmsPart mmsPart){
        ContentResolver contentResolver = CmsService.getInstance().getContext().getContentResolver();
        String nativeId = mmsPart.getNativeId();
        String baseId = mmsPart.getBaseId();
        Uri uri;
        if(nativeId!=null){
            uri = Uri.parse(XmsObserverUtils.Mms.Part.URI.concat(nativeId));
            return MmsUtils.createThumb(contentResolver, uri);
        }
        else if (baseId!=null){
            uri = Uri.withAppendedPath(PartData.CONTENT_URI, mmsPart.getBaseId());
            return MmsUtils.createThumb(contentResolver, uri);
        }

        return createThumb(mmsPart.getPath());
    }

    private static byte[] createThumb(String imagePath){
        Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePath), THUMB_SIZE, THUMB_SIZE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    private static byte[] createThumb(ContentResolver contentResolver, Uri uri){

        InputStream is = null;
        try{
            is = contentResolver.openInputStream(uri);
            Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(is), THUMB_SIZE, THUMB_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        finally {
            CloseableUtils.tryToClose(is);
        }
    }


    public static byte[] getContent(MmsPart mmsPart){
        ContentResolver contentResolver = CmsService.getInstance().getContext().getContentResolver();
        String nativeId = mmsPart.getNativeId();
        Uri uri;
        if(nativeId!=null){
            uri = Uri.parse(XmsObserverUtils.Mms.Part.URI.concat(nativeId));
        }
        else{
            uri = Uri.withAppendedPath(PartData.CONTENT_URI, mmsPart.getBaseId());
        }
        return getContent(contentResolver, uri);
    }

    private static byte[] getContent(ContentResolver contentResolver, Uri uri){

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

//    private static byte[] getContent(String filePath){
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        FileInputStream fis = null;
//        try{
//            fis = new FileInputStream(new File(filePath));
//            byte[] buf = new byte[1024];
//            int n;
//            while ((n=fis.read(buf))!=-1) {
//                baos.write(buf, 0, n);
//            }
//            return baos.toByteArray();
//        }
//        catch(Exception e){
//            return null;
//        }
//        finally {
//            if(fis!=null){
//                try {fis.close();} catch (IOException e) {e.printStackTrace();}
//            }
//        }
//    }

    public static String saveContent(String contentType, String contentId, byte[] data){

        String fileName = new StringBuilder(MMS_DIRECTORY_PATH).append(File.separator).
                append(contentId).append("_").
                append(DateUtils.getMmsFileDate(System.currentTimeMillis())).
                append(".").append(MimeManager.getInstance().getExtensionFromMimeType(contentType)).toString();
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(new File(fileName));
            bos = new BufferedOutputStream(fos, 8 * 1024);
            bos.write(data);
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally{
            CloseableUtils.tryToClose(bos);
            CloseableUtils.tryToClose(fos);
        }
    }

    public static void tryToDelete(String path){
        new File(path).delete();
    }
}
