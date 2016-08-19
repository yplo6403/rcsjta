/*
 * Copyright 2013 Jacob Klinker
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
 */

package com.klinker.android.send_message;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Class to hold all relevant message information to send
 *
 * @author Jake Klinker
 */
public class Message {

    public static final class Part {
        private final byte[] mMedia;
        private final String mContentType;
        private final String mName;

        public Part(byte[] media, String contentType, String name) {
            mMedia = media;
            mContentType = contentType;
            mName = name;
        }

        public byte[] getMedia() {
            return mMedia;
        }

        public String getContentType() {
            return mContentType;
        }

        public String getName() {
            return mName;
        }
    }

    private final String mText;
    private final String mSubject;
    private final String[] mAddresses;
    private final List<Part> mParts;
    private boolean mSave;
    private int mType;
    private int mDelay;

    /**
     * Default send type, to be sent through SMS or MMS depending on contents
     */
    public static final int TYPE_SMSMMS = 0;

    /**
     * Google Voice send type
     */
    public static final int TYPE_VOICE = 1;

    /**
     * Constructor
     *
     * @param text is the message to send
     * @param addresses is an array of phone numbers to send to
     */
    public Message(String text, String[] addresses) {
        mText = text;
        mAddresses = addresses;
        mSubject = null;
        mSave = true;
        mType = TYPE_SMSMMS;
        mDelay = 0;
        mParts = null;
    }

    /**
     * Constructor
     *
     * @param addresses is an array of phone numbers to send to
     * @param parts is list of parts that you want to send
     * @param subject is the subject of the mms message
     */
    public Message(String[] addresses, List<Part> parts, String subject) {
        mText = null;
        mAddresses = addresses;
        mSubject = subject;
        mSave = true;
        mType = TYPE_SMSMMS;
        mDelay = 0;
        mParts = parts;
    }

    /**
     * Gets the text of the message to send
     *
     * @return the string of the message to send
     */
    public String getText() {
        return mText;
    }

    /**
     * Gets the addresses of the message
     *
     * @return an array of strings with all of the addresses
     */
    public String[] getAddresses() {
        return mAddresses;
    }

    /**
     * Gets the audio sample in the message
     *
     * @return an array of bytes with audio information for the message
     */
    public List<Part> getParts() {
        return mParts;
    }

    /**
     * Gets the subject of the mms message
     *
     * @return a string with the subject of the message
     */
    public String getSubject() {
        return mSubject;
    }

    /**
     * Gets whether or not to save the message to the database
     *
     * @return a boolean of whether or not to save
     */
    public boolean getSave() {
        return mSave;
    }

    /**
     * Gets the time to delay before sending the message
     *
     * @return the delay time in milliseconds
     */
    public int getDelay() {
        return mDelay;
    }

    /**
     * Gets the type of message to be sent, see Message.TYPE_SMSMMS, Message.TYPE_FACEBOOK, or
     * Message.TYPE_VOICE
     *
     * @return the type of the message
     */
    public int getType() {
        return mType;
    }

    /**
     * Static method to convert a bitmap into a byte array to easily send it over http
     *
     * @param image is the image to convert
     * @return a byte array of the image data
     */
    public static byte[] bitmapToByteArray(Bitmap image) {
        if (image == null) {
            Log.v("Message", "image is null, returning byte array of size 0");
            return new byte[0];
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }
}
