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

package com.gsma.rcs.core.cms.xms.mms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.utils.MmsUtils;
import com.gsma.rcs.provider.xms.model.MmsDataObject;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* package private */final class MmsEncoder {

    public static final int MESSAGE_TYPE_SEND_REQ = 0x80;

    public static final int BCC = 0x81;
    public static final int CC = 0x82;
    public static final int CONTENT_LOCATION = 0x83;
    public static final int CONTENT_TYPE = 0x84;
    public static final int DATE = 0x85;
    public static final int DELIVERY_REPORT = 0x86;
    public static final int DELIVERY_TIME = 0x87;
    public static final int EXPIRY = 0x88;
    public static final int FROM = 0x89;
    public static final int MESSAGE_CLASS = 0x8A;
    public static final int MESSAGE_ID = 0x8B;
    public static final int MESSAGE_TYPE = 0x8C;
    public static final int MMS_VERSION = 0x8D;
    public static final int MESSAGE_SIZE = 0x8E;
    public static final int PRIORITY = 0x8F;
    public static final int READ_REPLY = 0x90;
    public static final int REPORT_ALLOWED = 0x91;
    public static final int RESPONSE_STATUS = 0x92;
    public static final int RESPONSE_TEXT = 0x93;
    public static final int SENDER_VISIBILITY = 0x94;
    public static final int STATUS = 0x95;
    public static final int SUBJECT = 0x96;
    public static final int TO = 0x97;
    public static final int TRANSACTION_ID = 0x98;

    public static final int FROM_ADDRESS_PRESENT_TOKEN = 0x80;
    public static final int FROM_INSERT_ADDRESS_TOKEN = 0x81;

    private static final Map<String, Integer> MMS_CONTENT_TYPES;

    static {
        MMS_CONTENT_TYPES = new HashMap<>();
        MMS_CONTENT_TYPES.put("*/*", 0x00);
        MMS_CONTENT_TYPES.put("text/*", 0x01);
        MMS_CONTENT_TYPES.put("text/html", 0x02);
        MMS_CONTENT_TYPES.put("text/plain", 0x03);
        MMS_CONTENT_TYPES.put("text/x-hdml", 0x04);
        MMS_CONTENT_TYPES.put("text/x-ttml", 0x05);
        MMS_CONTENT_TYPES.put("text/x-vCalendar", 0x06);
        MMS_CONTENT_TYPES.put("text/x-vCard", 0x07);
        MMS_CONTENT_TYPES.put("text/vnd.wap.wml", 0x08);
        MMS_CONTENT_TYPES.put("text/vnd.wap.wmlscript", 0x09);
        MMS_CONTENT_TYPES.put("text/vnd.wap.wta-event", 0x0A);
        MMS_CONTENT_TYPES.put("multipart/*", 0x0B);
        MMS_CONTENT_TYPES.put("multipart/mixed", 0x0C);
        MMS_CONTENT_TYPES.put("multipart/form-data", 0x0D);
        MMS_CONTENT_TYPES.put("multipart/byterantes", 0x0E);
        MMS_CONTENT_TYPES.put("multipart/alternative", 0x0F);
        MMS_CONTENT_TYPES.put("application/*", 0x10);
        MMS_CONTENT_TYPES.put("application/java-vm", 0x11);
        MMS_CONTENT_TYPES.put("application/x-www-form-urlencoded", 0x12);
        MMS_CONTENT_TYPES.put("application/x-hdmlc", 0x13);
        MMS_CONTENT_TYPES.put("application/vnd.wap.wmlc", 0x14);
        MMS_CONTENT_TYPES.put("application/vnd.wap.wmlscriptc", 0x15);
        MMS_CONTENT_TYPES.put("application/vnd.wap.wta-eventc", 0x16);
        MMS_CONTENT_TYPES.put("application/vnd.wap.uaprof", 0x17);
        MMS_CONTENT_TYPES.put("application/vnd.wap.wtls-ca-certificate", 0x18);
        MMS_CONTENT_TYPES.put("application/vnd.wap.wtls-user-certificate", 0x19);
        MMS_CONTENT_TYPES.put("application/x-x509-ca-cert", 0x1A);
        MMS_CONTENT_TYPES.put("application/x-x509-user-cert", 0x1B);
        MMS_CONTENT_TYPES.put("image/*", 0x1C);
        MMS_CONTENT_TYPES.put("image/gif", 0x1D);
        MMS_CONTENT_TYPES.put("image/jpeg", 0x1E);
        MMS_CONTENT_TYPES.put("image/tiff", 0x1F);
        MMS_CONTENT_TYPES.put("image/png", 0x20);
        MMS_CONTENT_TYPES.put("image/vnd.wap.wbmp", 0x21);
        MMS_CONTENT_TYPES.put("application/vnd.wap.multipart.*", 0x22);
        MMS_CONTENT_TYPES.put("application/vnd.wap.multipart.mixed", 0x23);
        MMS_CONTENT_TYPES.put("application/vnd.wap.multipart.form-data", 0x24);
        MMS_CONTENT_TYPES.put("application/vnd.wap.multipart.byteranges", 0x25);
        MMS_CONTENT_TYPES.put("application/vnd.wap.multipart.alternative", 0x26);
        MMS_CONTENT_TYPES.put("application/xml", 0x27);
        MMS_CONTENT_TYPES.put("text/xml", 0x28);
        MMS_CONTENT_TYPES.put("application/vnd.wap.wbxml", 0x29);
        MMS_CONTENT_TYPES.put("application/x-x968-cross-cert", 0x2A);
        MMS_CONTENT_TYPES.put("application/x-x968-ca-cert", 0x2B);
        MMS_CONTENT_TYPES.put("application/x-x968-user-cert", 0x2C);
        MMS_CONTENT_TYPES.put("text/vnd.wap.si", 0x2D);
        MMS_CONTENT_TYPES.put("application/vnd.wap.sic", 0x2E);
        MMS_CONTENT_TYPES.put("text/vnd.wap.sl", 0x2F);
        MMS_CONTENT_TYPES.put("application/vnd.wap.slc", 0x30);
        MMS_CONTENT_TYPES.put("text/vnd.wap.co", 0x31);
        MMS_CONTENT_TYPES.put("application/vnd.wap.coc", 0x32);
        MMS_CONTENT_TYPES.put("application/vnd.wap.multipart.related", 0x33);
        MMS_CONTENT_TYPES.put("application/vnd.wap.sia", 0x34);
        MMS_CONTENT_TYPES.put("text/vnd.wap.connectivity-xml", 0x35);
        MMS_CONTENT_TYPES.put("application/vnd.wap.connectivity-wbxml", 0x36);
        MMS_CONTENT_TYPES.put("application/pkcs7-mime", 0x37);
        MMS_CONTENT_TYPES.put("application/vnd.wap.hashed-certificate", 0x38);
        MMS_CONTENT_TYPES.put("application/vnd.wap.signed-certificate", 0x39);
        MMS_CONTENT_TYPES.put("application/vnd.wap.cert-response", 0x3A);
        MMS_CONTENT_TYPES.put("application/xhtml+xml", 0x3B);
        MMS_CONTENT_TYPES.put("application/wml+xml", 0x3C);
        MMS_CONTENT_TYPES.put("text/css", 0x3D);
        MMS_CONTENT_TYPES.put("application/vnd.wap.mms-message", 0x3E);
        MMS_CONTENT_TYPES.put("application/vnd.wap.rollover-certificate", 0x3F);
        MMS_CONTENT_TYPES.put("application/vnd.wap.locc+wbxml", 0x40);
        MMS_CONTENT_TYPES.put("application/vnd.wap.loc+xml", 0x41);
        MMS_CONTENT_TYPES.put("application/vnd.syncml.dm+wbxml", 0x42);
        MMS_CONTENT_TYPES.put("application/vnd.syncml.dm+xml", 0x43);
        MMS_CONTENT_TYPES.put("application/vnd.syncml.notification", 0x44);
        MMS_CONTENT_TYPES.put("application/vnd.wap.xhtml+xml", 0x45);
        MMS_CONTENT_TYPES.put("application/vnd.wv.csp.cir", 0x46);
        MMS_CONTENT_TYPES.put("application/vnd.oma.dd+xml", 0x47);
        MMS_CONTENT_TYPES.put("application/vnd.oma.drm.message", 0x48);
        MMS_CONTENT_TYPES.put("application/vnd.oma.drm.content", 0x49);
        MMS_CONTENT_TYPES.put("application/vnd.oma.drm.rights+xml", 0x4A);
        MMS_CONTENT_TYPES.put("application/vnd.oma.drm.rights+wbxml", 0x4B);
    }

    private final MmsEncodedMessage mMessage;
    private final Context mCtx;

    public MmsEncoder(Context ctx, MmsEncodedMessage message) {
        mCtx = ctx;
        mMessage = message;
    }

    /**
     * Encode the message to a valid mms message
     *
     * @return a byte array of the message
     */
    public byte[] encode() throws FileAccessException, MmsFormatException {
        try {
            ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
            // [WARNING] MANDATORY FIELD (TODO)
            outBuffer.write(MESSAGE_TYPE);
            outBuffer.write(mMessage.getType());

            // [WARNING] MANDATORY FIELD (TODO)
            outBuffer.write(TRANSACTION_ID);
            outBuffer.write(mMessage.getTransactionId().getBytes());
            outBuffer.write(0x0);

            // [WARNING] MANDATORY FIELD (TODO)
            outBuffer.write(MMS_VERSION);
            outBuffer.write(0x90);

            outBuffer.write(DATE);
            outBuffer.write(encodeDate(mMessage.getDate()));

            outBuffer.write(MESSAGE_CLASS);
            outBuffer.write(0x80);

            // Priority
            outBuffer.write(PRIORITY);
            outBuffer.write(0x81);

            // [WARNING] MANDATORY FIELD (TODO)
            outBuffer.write(FROM);
            String from = mMessage.getFrom();
            outBuffer.write(from.length() + 2);
            outBuffer.write(0x80);
            outBuffer.write(from.getBytes());
            outBuffer.write(0x0);

            for (String to : mMessage.getTo()) {
                outBuffer.write(TO);
                outBuffer.write(to.getBytes());
                outBuffer.write(0x0);
            }

            if (mMessage.getSubject() != null && mMessage.getSubject().length() > 0) {
                outBuffer.write(SUBJECT);
                outBuffer.write(mMessage.getSubject().getBytes());
                outBuffer.write(0x0);
            }

            // [WARNING] MANDATORY FIELD (TODO)
            outBuffer.write(CONTENT_TYPE);
            outBuffer.write(MMS_CONTENT_TYPES.get(mMessage.getContentType()) + 0x80);

            /* Write parts */
            List<MmsDataObject.MmsPart> parts = mMessage.getParts();
            outBuffer.write(encodeUint(parts.size()));
            for (MmsDataObject.MmsPart part : parts) {
                String mimeType = part.getMimeType();
                byte[] content;
                String body = part.getContentText();
                if (body != null) {
                    content = body.getBytes();
                } else {
                    content = MmsUtils.getContent(mCtx.getContentResolver(), part.getFile());
                }
                outBuffer.write(encodePart(content, mimeType));
            }
            return outBuffer.toByteArray();

        } catch (IOException ioe) {
            throw new MmsFormatException("Failed to encode MMS!", ioe);
        }
    }

    /**
     * Encode a timestamp to a valid mms format
     *
     * @param value : Timestamp
     * @return a byte array representing the date
     */
    private byte[] encodeDate(long value) {
        // Nothing to do if the value equals 0
        if (value == 0)
            return new byte[] {
                0x0
            };

        // We just need a timestamp in seconds not milliseconds
        value = value / 1000;

        // A long value is encoded on 8 bytes
        byte[] usefulValues = new byte[8];
        int useless = 0;

        // Mask each byte of the long number to match with the mms format
        for (int i = 1; i <= 8; i++) {
            long mask = ((0XFFFFFFFFFFFFFFFFL) >>> 64 - (8 * i));
            long temp = (value & mask) >>> (8 * (i - 1));
            usefulValues[i - 1] = (byte) temp;
            useless = (temp == 0) ? (useless + 1) : 0;
        }

        // Inverted loop to match the standard
        // We only get the useful values
        int size = 8 - useless;
        byte[] result = new byte[size];
        for (int i = size - 1; i >= 0; i--) {
            result[size - 1 - i] = usefulValues[i];
        }

        // The encoded date will be stored in this object
        byte[] resultFinal = new byte[result.length + 1];

        // First byte must be the length
        resultFinal[0] = (byte) result.length;

        // We can copy our byte array into the result one
        System.arraycopy(result, 0, resultFinal, 1, result.length);

        return resultFinal;
    }

    /**
     * Encode an integer to the mms format
     *
     * @param value : Integer
     * @return a byte array representing the integer
     */
    private byte[] encodeUint(int value) {
        // Nothing to do if the value equals 0
        if (value == 0)
            return new byte[] {
                0x0
            };

        // An integer is encoded on 5 bytes
        byte[] usefulValues = new byte[5];
        int useless = 0;

        // Mask each byte of the integer to match with the mms format
        for (int i = 1; i <= 5; i++) {
            int mask = ((0XFFFFFFFF) >>> 32 - (7 * i));
            int temp = (value & mask) >>> (7 * (i - 1));
            usefulValues[i - 1] = (byte) (0x80 | temp);
            useless = (temp == 0) ? (useless + 1) : 0;
        }

        // Header if integer
        usefulValues[0] &= 0x7F;

        // Inverted loop to match the standard
        // We only get the useful values
        int size = 5 - useless;
        byte[] result = new byte[size];
        for (int i = size - 1; i >= 0; i--) {
            result[size - 1 - i] = usefulValues[i];
        }
        return result;
    }

    /**
     * Encode the mms part to a PDU format
     *
     * @param content : The content to encode
     * @param mimeType The mime type content
     * @return a byte array of the encoded part
     */
    private byte[] encodePart(byte[] content, String mimeType) {
        // Define content type
        int contentType = MMS_CONTENT_TYPES.get(mimeType);
        byte[] contentTypeHeader = new byte[] {
            (byte) (contentType + 0x80)
        };

        // We retrieve the content of the part and it's length
        byte[] dataLengthEnc = encodeUint(content.length);

        // The result array will be stored in this object
        byte[] partEnc = new byte[2 + dataLengthEnc.length + content.length];
        partEnc[0] = 0x1;

        int offset = 1;
        System.arraycopy(dataLengthEnc, 0, partEnc, offset, dataLengthEnc.length);
        offset += dataLengthEnc.length;
        // We copy the content type header to the array
        System.arraycopy(contentTypeHeader, 0, partEnc, offset, 1);
        offset += 1;
        // We can now copy the content to the array
        System.arraycopy(content, 0, partEnc, offset, content.length);
        return partEnc;
    }
}
