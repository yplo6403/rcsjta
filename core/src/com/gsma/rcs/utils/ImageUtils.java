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

package com.gsma.rcs.utils;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.service.cms.mms.MmsFileSizeException;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private static final int THUMB_SIZE = 100;
    private static final int ICON_HEIGHT = 100;
    private static final int ICON_WIDTH = 100;
    /**
     * The quality parameter which is used to compress JPEG images.
     */
    public static final int IMAGE_COMPRESSION_QUALITY = 100;
    public static final int MINIMUM_IMAGE_COMPRESSION_QUALITY = 10;

    private static final int NUMBER_OF_RESIZE_ATTEMPTS = 4;

    private static Logger sLogger = Logger.getLogger(ImageUtils.class.getSimpleName());

    private static BitmapFactory.Options readImageOptions(String imagePath) {
        /*
         * Read the dimensions and type of the image data prior to construction (and memory
         * allocation) of the bitmap
         */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        /* Decode image just to display dimension */
        BitmapFactory.decodeFile(imagePath, options);
        return options;
    }

    private static byte[] getCompressedBitmap(String path, Bitmap image, long maxSize)
            throws MmsFileSizeException {
        /* Compress the file to be under the limit */
        int quality = IMAGE_COMPRESSION_QUALITY;
        ByteArrayOutputStream out;
        long size;
        do {
            out = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, quality, out);
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                throw new MmsFileSizeException("Failed to compress image " + path);
            }
            size = out.size();
            quality -= 10;
        } while (size > maxSize && quality > MINIMUM_IMAGE_COMPRESSION_QUALITY);
        if (quality < MINIMUM_IMAGE_COMPRESSION_QUALITY) {
            throw new MmsFileSizeException("Failed to compress image " + path
                    + " : quality too low! maxSize=" + maxSize);
        }
        if (sLogger.isActivated()) {
            sLogger.warn("Compress image " + path + " quality=" + (quality + 10) + "/100. maxSize="
                    + maxSize + " shrinkedSize=" + size);
        }
        return out.toByteArray();
    }

    /**
     * Compress image
     * 
     * @param imagePath the image path
     * @param maxSize the maximum compressed image size
     * @param maxWidth the maximum compressed image width
     * @param maxHeight the maximum compressed image height
     * @return the compressed image
     * @throws MmsFileSizeException
     * @throws FileAccessException
     */
    public static byte[] compressImage(String imagePath, long maxSize, int maxWidth, int maxHeight)
            throws MmsFileSizeException, FileAccessException {
        BitmapFactory.Options options = readImageOptions(imagePath);
        /* Calculate the reduction factor */
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        int loopCount = 1;
        for (; loopCount++ <= NUMBER_OF_RESIZE_ATTEMPTS;) {
            try {
                if (sLogger.isActivated()) {
                    sLogger.debug("bitmap: " + imagePath + " Sample factor=" + options.inSampleSize);
                }
                /* Rotate image is orientation is not 0 degree */
                Bitmap bitmapTmp = BitmapFactory.decodeFile(imagePath, options);
                if (bitmapTmp == null) {
                    return null;
                }
                int degree = getExifOrientation(imagePath);
                if (degree == 0) {
                    return getCompressedBitmap(imagePath, bitmapTmp, maxSize);
                }
                bitmapTmp = rotateBitmap(bitmapTmp, degree);
                if (bitmapTmp != null) {
                    return getCompressedBitmap(imagePath, bitmapTmp, maxSize);
                }
            } catch (OutOfMemoryError e) {
                /*
                 * If an OutOfMemoryError occurred, we continue with for loop and next inSampleSize
                 * value
                 */
                if (sLogger.isActivated()) {
                    sLogger.warn("OutOfMemoryError: options.inSampleSize= " + options.inSampleSize);
                }
                options.inSampleSize++;
            }
        }
        throw new MmsFileSizeException("Failed to compress image " + imagePath
                + " : too many attempts! maxSize=" + maxSize);
    }

    /**
     * Try to create a thumbnail from the image path
     * 
     * @param imagePath the image path
     * @param maxSize the maximum thumbnail size
     * @return the thumbnail or null if thumbnail creation fails
     */
    public static byte[] tryGetThumbnail(String imagePath, long maxSize) {
        try {
            BitmapFactory.Options options = readImageOptions(imagePath);
            /* Calculate the reduction factor */
            options.inSampleSize = calculateInSampleSize(options, ICON_WIDTH, ICON_HEIGHT);
            options.inJustDecodeBounds = false;
            int loopCount = 1;
            for (; loopCount++ <= 4;) {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("bitmap: " + imagePath + " Sample factor: "
                                + options.inSampleSize);
                    }
                    /* Rotate image is orientation is not 0 degree */
                    Bitmap bitmapTmp = BitmapFactory.decodeFile(imagePath, options);
                    if (bitmapTmp == null) {
                        return null;
                    }
                    int degree = getExifOrientation(imagePath);
                    if (degree == 0) {
                        return getCompressedBitmap(imagePath, bitmapTmp, maxSize);
                    }
                    bitmapTmp = rotateBitmap(bitmapTmp, degree);
                    if (bitmapTmp != null) {
                        return getCompressedBitmap(imagePath, bitmapTmp, maxSize);
                    }
                } catch (OutOfMemoryError e) {
                    /*
                     * If an OutOfMemoryError occurred, we continue with for loop and next
                     * inSampleSize value
                     */
                    if (sLogger.isActivated()) {
                        sLogger.warn("OutOfMemoryError: options.inSampleSize= "
                                + options.inSampleSize);
                    }
                    options.inSampleSize++;
                }
            }
            return null;
        } catch (MmsFileSizeException e) {
            sLogger.warn("Failed to create thumbnail for : " + imagePath);
            return null;
        }
    }

    /**
     * Calculate the sub-sampling factor to load image on screen Image will be sized to be the
     * smallest possible, keeping it bigger than requested size. No resize factor will be applied if
     * one or both dimensions are smallest than requested. If width and height are both bigger than
     * requested, ensure final image will have both dimensions larger than or equal to the requested
     * height and width
     * <p/>
     * A requested dimension set to 1 pixel will let its size to adapt with no constraints.
     *
     * @param options contains the raw height and width of image
     * @param reqWidth the target width
     * @param reqHeight the target height
     * @return the sub-sampling factor
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        /* Raw height and width of image */
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            /*
             * Calculate the largest inSampleSize value that is a power of 2 and keeps both height
             * and width larger than the requested height and width.
             */
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Read the EXIF data from a given file to know the corresponding rotation, if any
     *
     * @param filename The filename
     * @return rotation in degree
     */
    private static int getExifOrientation(String filename) {
        // Also check if the image has to be rotated by reading metadata
        try {
            ExifInterface exif = new ExifInterface(filename);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        return 90;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        return 180;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        return 270;
                }
            }
            return 0;
        } catch (IOException ex) {
            if (sLogger.isActivated()) {
                sLogger.warn("Cannot read exif", ex);
            }
            return 0;
        }
    }

    /**
     * Rotates the bitmap by the specified degree If a new bitmap is created, the original bitmap is
     * recycled.
     *
     * @param b bitmap
     * @param degrees number of degree to rotate
     * @return bitmap
     */
    private static Bitmap rotateBitmap(Bitmap b, int degrees) {
        if (sLogger.isActivated()) {
            sLogger.debug("Rotate bitmap degrees: " + degrees);
        }
        Matrix m = new Matrix();
        m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
        try {
            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (b != b2) {
                b.recycle();
                b = b2;
            }
            return b;
        } catch (OutOfMemoryError ex) {
            if (sLogger.isActivated()) {
                // We have no memory to rotate. Return the original bitmap.
                sLogger.warn("OutOfMemoryError: cannot rotate image");
            }
            System.gc();
            return b;
        }
    }

    /**
     * Try to create thumbnail from URI
     * 
     * @param resolver the content resolver
     * @param file the file Uri
     * @return the thumbnail or null if thumbnail creation failed
     */
    public static byte[] tryGetThumbnail(ContentResolver resolver, Uri file) {
        InputStream is = null;
        try {
            is = resolver.openInputStream(file);
            Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(is),
                    THUMB_SIZE, THUMB_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();

        } catch (FileNotFoundException e) {
            sLogger.warn("Failed to create thumbnail for : " + file);
            return null;

        } finally {
            // noinspection ThrowableResultOfMethodCallIgnored
            CloseableUtils.tryToClose(is);
        }
    }
}
