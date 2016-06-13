/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.cms.utils;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.xms.observer.XmsObserverUtils;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ImageUtils;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.cms.XmsMessage;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
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

public class MmsUtils {

    // @formatter:off
    public static final String[] PROJECTION = new String[]{
            Telephony.BaseMmsColumns._ID,
            Telephony.BaseMmsColumns.SUBJECT,
            Telephony.BaseMmsColumns.MESSAGE_ID,
            Telephony.BaseMmsColumns.MESSAGE_TYPE,
            Telephony.BaseMmsColumns.THREAD_ID,
            Telephony.BaseMmsColumns.DATE,
            Telephony.BaseMmsColumns.READ
    };
    // @formatter:on

    private static final Logger sLogger = Logger.getLogger(MmsUtils.class.getName());

    private static final int MAX_BUF_READ_SIZE = 1024;

    private static final String WHERE_MSG_ID_NOT_NULL = Telephony.BaseMmsColumns.MESSAGE_ID
            + " is not null";

    private static final String WHERE_INBOX = Telephony.BaseMmsColumns.MESSAGE_BOX + "="
            + Telephony.BaseMmsColumns.MESSAGE_BOX_INBOX;

    private static final String WHERE_SENT = Telephony.BaseMmsColumns.MESSAGE_BOX + "="
            + Telephony.BaseMmsColumns.MESSAGE_BOX_SENT;

    public static final String WHERE_INBOX_OR_SENT = WHERE_MSG_ID_NOT_NULL + " AND (" + WHERE_INBOX
            + " OR " + WHERE_SENT + " )";

    private static final String WHERE_INBOX_OR_SENT_WITH_ID = WHERE_INBOX_OR_SENT + " AND "
            + BaseColumns._ID + "=?";

    private final static int MMS_TYPE_SEND_REQUEST = 128;

    private static Set<ContactId> getMmsContacts(ContentResolver contentResolver, Long id,
            RcsService.Direction dir) {
        Set<ContactId> contacts = new HashSet<>();
        Cursor cursor = null;
        try {
            int type = XmsObserverUtils.Mms.Addr.FROM;
            if (dir == RcsService.Direction.OUTGOING) {
                type = XmsObserverUtils.Mms.Addr.TO;
            }
            cursor = contentResolver.query(
                    Uri.parse(String.format(XmsObserverUtils.Mms.Addr.URI, id)),
                    XmsObserverUtils.Mms.Addr.PROJECTION, XmsObserverUtils.Mms.Addr.WHERE,
                    new String[] {
                        String.valueOf(type)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsObserverUtils.Mms.Addr.URI);
            int adressIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS);
            while (cursor.moveToNext()) {
                String address = cursor.getString(adressIdx);
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

    /**
     * Gets the MMS from the native provider
     * 
     * @param contentResolver the content resolver
     * @param id the native ID
     * @param ntpLocalOffset the time offset to apply
     * @return the list of MMS object because if sent to mulptiple contacts, MMS pdu is duplicated.
     */

    public static List<MmsDataObject> getMmsFromNativeProvider(ContentResolver contentResolver,
            Long id, long ntpLocalOffset) {
        List<MmsDataObject> mmsDataObject = new ArrayList<>();
        Long threadId, date;
        date = -1L;
        String mmsId;
        String subject;
        RcsService.Direction direction = RcsService.Direction.INCOMING;

        RcsService.ReadStatus readStatus;
        Cursor cursor = null;
        String where = XmsObserverUtils.Mms.WHERE_INBOX_OR_SENT;
        String[] whereArgs = null;
        if (id != null) {
            where = WHERE_INBOX_OR_SENT_WITH_ID;
            whereArgs = new String[] {
                String.valueOf(id)
            };
        }
        /* Firstly get the PDU information */
        try {
            cursor = contentResolver.query(XmsObserverUtils.Mms.URI, PROJECTION, where, whereArgs,
                    Telephony.BaseMmsColumns._ID.concat(" DESC"));
            CursorUtil.assertCursorIsNotNull(cursor, XmsObserverUtils.Mms.URI);
            if (!cursor.moveToNext()) {
                return mmsDataObject;
            }
            if (id == null) {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            }
            threadId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.THREAD_ID));
            mmsId = cursor.getString(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.MESSAGE_ID));
            readStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns.READ)) == 0 ? RcsService.ReadStatus.UNREAD
                    : RcsService.ReadStatus.READ;
            int messageType = cursor.getInt(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.MESSAGE_TYPE));
            subject = cursor.getString(cursor
                    .getColumnIndexOrThrow(Telephony.BaseMmsColumns.SUBJECT));
            if (MMS_TYPE_SEND_REQUEST == messageType) {
                direction = RcsService.Direction.OUTGOING;
                // Outgoing message are always read
                readStatus = RcsService.ReadStatus.READ;
            }
            /*
             * Curiously MMS are timestamped in sec whereas SMS use msec.
             */
            date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns.DATE)) * 1000;
            date += ntpLocalOffset;

        } finally {
            CursorUtil.close(cursor);
        }
        /* Get recipients and associate a message Id */
        Set<ContactId> contacts = MmsUtils.getMmsContacts(contentResolver, id, direction);
        if (contacts.isEmpty()) {
            if (sLogger.isActivated()) {
                sLogger.warn("Failed to read contacts for MMS id=" + mmsId);
            }
            return mmsDataObject;
        }
        /* Secondly get parts and duplicate for all recipients */
        List<MmsDataObject.MmsPart> parts = MmsUtils.getMmsParts(contentResolver, id, mmsId);
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
            state = XmsMessage.State.SENT;
        }
        for (ContactId contact : contacts) {
            MmsDataObject mms = new MmsDataObject(mmsId, contact, subject, direction, readStatus,
                    date, id, threadId, parts);
            mms.setState(state);
            mmsDataObject.add(mms);
        }
        return mmsDataObject;
    }

    private static List<MmsDataObject.MmsPart> getMmsParts(ContentResolver contentResolver,
            Long id, String mmsId) {
        List<MmsDataObject.MmsPart> parts = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(Uri.parse(XmsObserverUtils.Mms.Part.URI),
                    XmsObserverUtils.Mms.Part.PROJECTION, XmsObserverUtils.Mms.Part.WHERE,
                    new String[] {
                        String.valueOf(id)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, XmsObserverUtils.Mms.Part.URI);
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
                    Uri file = Uri.parse(XmsObserverUtils.Mms.Part.URI.concat(cursor
                            .getString(_idIdx)));
                    byte[] bytes;
                    try {
                        bytes = MmsUtils.getContent(contentResolver, file);

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
                        fileIcon = ImageUtils.tryGetThumbnail(contentResolver, file);
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

    public static byte[] getContent(ContentResolver contentResolver, Uri file)
            throws FileAccessException {
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream is = contentResolver.openInputStream(file);
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
}
