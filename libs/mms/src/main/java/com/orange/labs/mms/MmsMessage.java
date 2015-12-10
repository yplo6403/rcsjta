/*
 *
 * MmsLib
 * 
 * Module name: com.orange.labs.mms
 * Version:     2.0
 * Created:     2013-06-06
 * Author:      vkxr8185 (Yoann Hamon)
 * 
 * Copyright (C) 2013 Orange
 *
 * This software is confidential and proprietary information of France Telecom Orange.
 * You shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 */

package com.orange.labs.mms;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.util.Log;

import com.orange.labs.mms.priv.MmsFileSizeException;
import com.orange.labs.mms.priv.PartMMS;
import com.orange.labs.mms.priv.utils.Constants;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class MmsMessage
{
	private String subject = null;
	private List<String> to = null;
	private List<PartMMS> parts = null;
	private PendingIntent intent = null;
	private boolean autoInsert = false;

    /**
     * The quality parameter which is used to compress JPEG images.
     */
    public static final int IMAGE_COMPRESSION_QUALITY = 95;
    /**
     * The minimum quality parameter which is used to compress JPEG images.
     */
    public static final int MINIMUM_IMAGE_COMPRESSION_QUALITY = 10;
    private static final int NUMBER_OF_RESIZE_ATTEMPTS = 20;
	
	public MmsMessage()
	{
		to = new ArrayList<String>();
		parts = new ArrayList<PartMMS>();
	}
	
	/**
	 * Add a recipient's number to the message
	 * @param number : recipient's number
	 * @return An instance of the current message
	 */
	public MmsMessage addTo(String number)
	{
		to.add(number);
		return this;
	}
	
	/**
	 * Set a subject to the message
	 * @param subject : Subject of the message
	 * @return An instance of the current message
	 */
	public MmsMessage setSubject(String subject)
	{
		this.subject = subject;
		return this;
	}
	
	/**
	 * Attach a file to the message
	 * @param mimeType : MIME Type of the file
	 * @param content : An byte array containing the file
	 * @return An instance of the current message
	 * @throws Exception 
	 */
	public MmsMessage attach(String mimeType, byte[] content) throws MmsFileSizeException
	{
		// Check size
		int totalSize = 0;
		for (PartMMS part : parts)
		{
			if (part.getContent() != null) totalSize += part.getContent().length;
		}
		if (totalSize + content.length > Constants.MAX_FILE_SIZE)
		{
            if (Constants.DEBUG) Log.w(Constants.TAG, "File is too big to be attached : total size is now " + (totalSize + content.length) + ", limit is " + Constants.MAX_FILE_SIZE);
			throw new MmsFileSizeException("File is too big to be attached : total size is now " + (totalSize + content.length) + ", limit is " + Constants.MAX_FILE_SIZE);
		}
		
		// Add the element to the message
		parts.add(new PartMMS(mimeType, content));
		return this;
	}

    /**
	 * Attach a file to the message
	 * @param bitmap : A bitmap to attach to the message
	 * @return An instance of the current message
	 * @throws Exception
	 */
	public MmsMessage attachBitmap(Bitmap bitmap) throws MmsFileSizeException
	{
        byte[] image = bitmapToBytes(bitmap,Constants.MAX_FILE_SIZE);
        this.attach("image/jpeg", image); // Attach the byte
        return this;
	}
	
	/**
	 * Set the pending intent that will be triggered when the message will be sent
	 * @param intent : An instance of the pending intent
	 * @return An instance of the current message
	 */
	public MmsMessage setPendingIntent(PendingIntent intent)
	{
		this.intent = intent;
		return this;
	}
	
	/**
	 * The message will be automatically inserted in the database if the param is true
	 * @param autoInsert : If true, the message will be automatically inserted in base
	 * @return An instance of the current message
	 */
	public MmsMessage setAutoInsert(boolean autoInsert)
	{
		this.autoInsert = autoInsert;
		return this;
	}
	
	/**
	 * Return a list of recipients
	 * @return A list of recipients
	 */
	public List<String> getTo()
	{
		return to;
	}
	
	/**
	 * Return the subject of the message
	 * @return The subject (if any)
	 */
	public String getSubject()
	{
		return subject;
	}
	
	/**
	 * Return a list of the parts included in the message
	 * @return A list of part
	 */
	public List<PartMMS> getParts()
	{
		return parts;
	}
	
	/**
	 * Return the pending intent associated with the message
	 * @return An instance of the pending intent
	 */
	public PendingIntent getPendingIntent()
	{
		return intent;
	}
	
	/**
	 * Return if the message should be automatically inserted in base or not
	 * @return true if the message should be inserted in base, false otherwise
	 */
	public boolean isAutoInsert()
	{
		return autoInsert;
	}

    private static byte[] bitmapToBytes(Bitmap bmp,int byteLimit)
    {
        byte[] result = null;
        boolean resultTooBig = true;
        int attempts = 0;   // reset count for second loop
        int quality = IMAGE_COMPRESSION_QUALITY;
        ByteArrayOutputStream bos = null;
        try
        {

            do {
                bos = new ByteArrayOutputStream();
                try {
                    // Compress the image into a JPG. Start with IMAGE_COMPRESSION_QUALITY.
                    // In case that the image byte size is still too large reduce the quality in
                    // proportion to the desired byte size.
                    try {
                        bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);
                    } catch (Exception e) {
                        if (Constants.DEBUG) Log.d(Constants.TAG, "Exception: " + e);
                        e.printStackTrace();
                    }
                    int jpgFileSize = bos.size();
                    if (Constants.DEBUG) Log.d(Constants.TAG, "compressed with quality set to " + quality + " and reached acceptable siz of "+bos.size() + " (limit is "+ byteLimit);
                    if (jpgFileSize > byteLimit) {
                        quality = (quality * byteLimit) / jpgFileSize;  // watch for int division!
                        if (quality < MINIMUM_IMAGE_COMPRESSION_QUALITY) {
                            quality = MINIMUM_IMAGE_COMPRESSION_QUALITY;
                        }
                    }
                } catch (OutOfMemoryError e) {
                    if (Constants.DEBUG) Log.e(Constants.TAG, "Unable to rezise image: " + e.getLocalizedMessage());
                    // fall through and keep trying with a smaller scale factor.
                }
                resultTooBig = bos == null || bos.size() > byteLimit;
                attempts++;
            } while (resultTooBig && attempts < NUMBER_OF_RESIZE_ATTEMPTS);
            result = bos.toByteArray();
            if (Constants.DEBUG) Log.d(Constants.TAG, "compressed to fit within boundaries, returning byte array");
            bos.close();
            bmp.recycle();
        }
        catch (Exception e)
        {
            if (Constants.DEBUG) Log.e(Constants.TAG, "something went terribly wrong Dave: " + e);
            e.printStackTrace();
        }

        return resultTooBig ? null : result;
    }
}
