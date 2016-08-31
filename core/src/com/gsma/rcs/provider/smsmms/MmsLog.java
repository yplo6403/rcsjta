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
 *
 ******************************************************************************/

package com.gsma.rcs.provider.smsmms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.xms.mms.MmsFileSizeException;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

import com.google.android.mms.pdu_alt.PduHeaders;
import com.klinker.android.send_message.Message;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to manage access to the native MMS provider.
 * 
 * @author Philippe LEMORDANT
 */
public class MmsLog implements IMmsLog {

    private static final Logger sLogger = Logger.getLogger(MmsLog.class.getName());

    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH = 640;
    private static final int MMS_PARTS_MAX_FILE_SIZE = 200000; /* International limit */

    private static final int MAX_BUF_READ_SIZE = 1024;
    private final Context mCtx;

    public static class Mms {

        public static class Pdu {

            public final static Uri URI = Uri.parse("content://mms/");

            // @formatter:off
            public static final String[] PROJECTION = new String[]{
                    Telephony.BaseMmsColumns._ID,
                    Telephony.BaseMmsColumns.SUBJECT,
                    Telephony.BaseMmsColumns.MESSAGE_ID,
                    Telephony.BaseMmsColumns.MESSAGE_TYPE,
                    Telephony.BaseMmsColumns.THREAD_ID,
                    Telephony.BaseMmsColumns.DATE,
                    Telephony.BaseMmsColumns.DATE_SENT,
                    Telephony.BaseMmsColumns.READ,
                    Telephony.BaseMmsColumns.MESSAGE_BOX
            };
            // @formatter:on

            public static final String WHERE = Telephony.BaseMmsColumns._ID + "=?";
        }

        public static class Addr {

            public static final String URI = "content://mms/%1$s/addr/";

            public static final String[] PROJECTION = new String[] {
                Telephony.Mms.Addr.ADDRESS
            };
            public static final String WHERE = Telephony.Mms.Addr.TYPE + "=?";
        }

        public static class Part {

            public static final Uri URI = Uri.parse("content://mms/part/");

            // @formatter:off
            public static final String[] PROJECTION = new String[] {
                    Telephony.BaseMmsColumns._ID,
                    Telephony.Mms.Part.CONTENT_TYPE,
                    Telephony.Mms.Part.TEXT,
                    Telephony.Mms.Part.CONTENT_LOCATION,
                    Telephony.Mms.Part._DATA
            };
            // @formatter:on

            public static final String WHERE = Telephony.Mms.Part.MSG_ID + "=?";
        }
    }

    private final ContentResolver mContentResolver;

    /* package private */MmsLog(Context ctx, ContentResolver resolver) {
        mCtx = ctx;
        mContentResolver = resolver;
    }

    private Set<ContactId> getMmsContacts(Long id, RcsService.Direction dir) {
        Set<ContactId> contacts = new HashSet<>();
        Cursor cursor = null;
        try {
            int type = PduHeaders.FROM;
            if (dir == RcsService.Direction.OUTGOING) {
                type = PduHeaders.TO;
            }
            cursor = mContentResolver.query(Uri.parse(String.format(Mms.Addr.URI, id)),
                    Mms.Addr.PROJECTION, Mms.Addr.WHERE, new String[] {
                        String.valueOf(type)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, Mms.Addr.URI);
            int addressIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS);
            while (cursor.moveToNext()) {
                String address = cursor.getString(addressIdx);
                ContactUtil.PhoneNumber phoneNumber = ContactUtil
                        .getValidPhoneNumberFromAndroid(address);
                if (phoneNumber == null) {
                    if (sLogger.isActivated()) {
                        sLogger.info("Bad format for contact : ".concat(address));
                    }
                    continue;
                }
                ContactId contact = ContactUtil.createContactIdFromValidatedData(phoneNumber);
                contacts.add(contact);
            }
            return contacts;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    private List<MmsDataObject.MmsPart> getMmsParts(Long id, String mmsId) {
        List<MmsDataObject.MmsPart> parts = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(Mms.Part.URI, Mms.Part.PROJECTION, Mms.Part.WHERE,
                    new String[] {
                        String.valueOf(id)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, Mms.Part.URI);
            int _idIdx = cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns._ID);
            int filenameIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_LOCATION);
            int contentTypeIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE);
            int textIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part.TEXT);
            int dataIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Part._DATA);
            while (cursor.moveToNext()) {
                String contentType = cursor.getString(contentTypeIdx);
                String text = cursor.getString(textIdx);
                String filename = cursor.getString(filenameIdx);
                String data = cursor.getString(dataIdx);
                if (contentType == null) { // skip MMS with null content type
                    return parts;
                }
                if (data != null) {
                    Uri file = ContentUris.withAppendedId(Mms.Part.URI, cursor.getInt(_idIdx));
                    byte[] bytes;
                    try {
                        bytes = readContent(mContentResolver, file);

                    } catch (FileAccessException e) {
                        if (sLogger.isActivated()) {
                            sLogger.warn(
                                    "Failed to read MMS part from native provider URI=" + file, e);
                        }
                        /* Skip invalid record */
                        continue;
                    }
                    Long fileSize = (long) bytes.length;
                    byte[] fileIcon = null;
                    if (MimeManager.isImageType(contentType)) {
                        fileIcon = ImageUtils.tryGetThumbnail(mContentResolver, file);
                    }
                    parts.add(new MmsDataObject.MmsPart(mmsId, filename, fileSize, contentType,
                            file, fileIcon));

                } else {
                    parts.add(new MmsDataObject.MmsPart(mmsId, contentType, text));
                }
            }
        } finally {
            CursorUtil.close(cursor);
        }
        return parts;
    }

    @Override
    public List<MmsDataObject> getMmsFromNativeProvider(long id, long ntpLocalOffset) {
        List<MmsDataObject> mmsDataObject = new ArrayList<>();
        String mmsId;
        RcsService.Direction direction = RcsService.Direction.INCOMING;
        RcsService.ReadStatus readStatus;
        String subject;
        Long date;
        Long dateSent;
        int msgBox;
        Cursor cursor = null;
        /* Firstly get the PDU information */
        try {
            cursor = mContentResolver.query(Mms.Pdu.URI, Mms.Pdu.PROJECTION, Mms.Pdu.WHERE,
                    new String[] {
                        String.valueOf(id)
                    }, Telephony.BaseMmsColumns._ID.concat(" DESC"));
            CursorUtil.assertCursorIsNotNull(cursor, Mms.Pdu.URI);
            if (!cursor.moveToNext()) {
                return mmsDataObject;
            }
            mmsId = cursor.getString(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.MESSAGE_ID));
            readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns.READ)) == 0 ? RcsService.ReadStatus.UNREAD
                    : RcsService.ReadStatus.READ;
            int messageType = cursor.getInt(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.MESSAGE_TYPE));
            subject = cursor.getString(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.SUBJECT));
            msgBox = cursor.getInt(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.MESSAGE_BOX));
            if (PduHeaders.MESSAGE_TYPE_SEND_REQ == messageType) {
                direction = RcsService.Direction.OUTGOING;
                // Outgoing message are always read
                readStatus = RcsService.ReadStatus.READ;
            }
            /*
             * Curiously MMS are timestamped in sec whereas SMS use msec.
             */
            date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns.DATE)) * 1000;
            date += ntpLocalOffset;
            dateSent = cursor.getLong(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.DATE_SENT));
            if (dateSent != 0) {
                dateSent += ntpLocalOffset;
            }
        } finally {
            CursorUtil.close(cursor);
        }
        /* Get recipients and associate a message Id */
        Set<ContactId> contacts = getMmsContacts(id, direction);
        if (contacts.isEmpty()) {
            if (sLogger.isActivated()) {
                sLogger.warn("Failed to read contacts for MMS id=" + mmsId);
            }
            return mmsDataObject;
        }
        /* Secondly get parts and duplicate for all recipients */
        List<MmsDataObject.MmsPart> parts = getMmsParts(id, mmsId);
        if (parts.isEmpty()) {
            if (sLogger.isActivated()) {
                sLogger.warn("Failed to read parts for MMS id=" + mmsId);
            }
            return mmsDataObject;
        }
        XmsMessage.State state;
        if (RcsService.Direction.INCOMING == direction) {
            state = (readStatus == RcsService.ReadStatus.READ ? XmsMessage.State.DISPLAYED
                    : XmsMessage.State.RECEIVED);
        } else {
            switch (msgBox) {
                case Telephony.Mms.MESSAGE_BOX_OUTBOX:
                    state = XmsMessage.State.QUEUED;
                    break;

                case Telephony.Mms.MESSAGE_BOX_FAILED:
                    state = XmsMessage.State.FAILED;
                    break;

                case Telephony.Mms.MESSAGE_BOX_SENT:
                    state = XmsMessage.State.SENT;
                    break;

                default:
                    if (sLogger.isActivated()) {
                        sLogger.warn("Invalid message box (" + msgBox + " for MMS id=" + mmsId);
                    }
                    return mmsDataObject;
            }
        }
        for (ContactId contact : contacts) {
            MmsDataObject mms = new MmsDataObject(mmsId, contact, subject, direction, readStatus,
                    date, id, parts);
            mms.setState(state);
            mms.setTimestampSent(dateSent);
            mmsDataObject.add(mms);
        }
        return mmsDataObject;
    }

    /**
     * Reads the content of a file uri
     *
     * @param resolver the content resolver
     * @param file the file uri
     * @return byte array
     * @throws FileAccessException
     */
    public static byte[] readContent(ContentResolver resolver, Uri file) throws FileAccessException {
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream is = resolver.openInputStream(file);
            if (is == null) {
                throw new FileAccessException("Failed to read Uri=" + file);
            }
            bis = new BufferedInputStream(is);
            byte[] buf = new byte[MAX_BUF_READ_SIZE];
            int n;
            while ((n = bis.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();

        } catch (IOException e) {
            throw new FileAccessException("Failed to read Uri=" + file, e);

        } finally {
            CloseableUtils.tryToClose(bis);
        }
    }

    /**
     * Save MMS to file system
     *
     * @param rcsSettings the RCS settings accessor
     * @param mimeType the mime type
     * @param mmsFileName the MMS file name
     * @param data the MMS data
     * @return the file URI
     * @throws FileAccessException
     */
    public static Uri saveContent(RcsSettings rcsSettings, String mimeType, String mmsFileName,
            byte[] data) throws FileAccessException {
        Uri uri = ContentManager.generateUriForReceivedContent(mmsFileName, mimeType, rcsSettings);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            File file = new File(uri.getPath());
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos, 8 * 1024);
            bos.write(data);
            return Uri.fromFile(file);

        } catch (IOException e) {
            throw new FileAccessException("Failed to save MMS file=" + mmsFileName, e);

        } finally {
            CloseableUtils.tryToClose(bos);
            CloseableUtils.tryToClose(fos);
        }
    }

    @Override
    public Message getMms(ContactId contact, List<Uri> files, final String subject,
            final String body) throws MmsFileSizeException, FileAccessException {
        List<Message.Part> parts = new ArrayList<>();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx);
        int maxImageWidth = sharedPreferences.getInt("mms_max_width", MAX_IMAGE_WIDTH);
        int maxImageHeight = sharedPreferences.getInt("mms_max_height", MAX_IMAGE_HEIGHT);
        long availableSize = Integer.valueOf(sharedPreferences.getString("mms_max_size",
                String.valueOf(MMS_PARTS_MAX_FILE_SIZE)));
        /* Remove size of the body text */
        if (body != null) {
            Message.Part part = new Message.Part(body.getBytes(), "text/plain", "body");
            parts.add(part);
            availableSize -= body.length();
        }
        /* remove size of the un-compressible parts */
        long imageSize = 0;
        for (Uri file : files) {
            String filename = FileUtils.getFileName(mCtx, file);
            long fileSize = FileUtils.getFileSize(mCtx, file);
            String extension = MimeManager.getFileExtension(filename);
            String mimeType = MimeManager.getInstance().getMimeType(extension);
            /* The part is a file */
            if (!MimeManager.isImageType(mimeType)) {
                /* The part cannot be compressed: not an image */
                availableSize -= fileSize;
                Message.Part part = new Message.Part(readContent(mContentResolver, file), mimeType,
                        filename);
                parts.add(part);
            } else {
                imageSize += fileSize;
            }
        }
        if (availableSize < 0) {
            throw new MmsFileSizeException("Sum of un-compressible MMS parts is too high!");
        }
        boolean compressionRequired = (imageSize > availableSize);
        for (Uri file : files) {
            String filename = FileUtils.getFileName(mCtx, file);
            long fileSize = FileUtils.getFileSize(mCtx, file);
            String extension = MimeManager.getFileExtension(filename);
            String mimeType = MimeManager.getInstance().getMimeType(extension);
            if (!MimeManager.isImageType(mimeType)) {
                continue;
            }
            if (compressionRequired) {
                Long targetSize = (fileSize * availableSize) / imageSize;
                String imagePath = FileUtils.getPath(mCtx, file);
                byte[] pdu = ImageUtils.compressImage(imagePath, targetSize, maxImageWidth,
                        maxImageHeight);
                // images are compressed in jpeg format
                Message.Part part = new Message.Part(pdu, "image/jpeg", filename);
                parts.add(part);
            } else {
                Message.Part part = new Message.Part(readContent(mContentResolver, file), mimeType,
                        filename);
                parts.add(part);
            }
        }
        return new Message(new String[] {
            contact.toString()
        }, parts, subject);
    }

    @Override
    public int deleteMms(long nativeId) {
        Uri uri = ContentUris.withAppendedId(Mms.Pdu.URI, nativeId);
        return mContentResolver.delete(uri, null, null);
    }

    @Override
    public boolean markMmsAsRead(Long nativeId) {
        if (sLogger.isActivated()) {
            sLogger.debug("markMmsAsRead nativeId=" + nativeId);
        }
        Uri uri = ContentUris.withAppendedId(Mms.Pdu.URI, nativeId);
        ContentValues values = new ContentValues();
        values.put(Telephony.TextBasedSmsColumns.READ, 1);
        return mContentResolver.update(uri, values, null, null) > 0;
    }
}
