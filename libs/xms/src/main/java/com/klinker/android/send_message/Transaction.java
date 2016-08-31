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

import com.android.mms.dom.smil.parser.SmilXmlSerializer;
import com.android.mms.service_alt.MmsNetworkManager;
import com.android.mms.service_alt.MmsRequestManager;
import com.android.mms.service_alt.SendRequest;
import com.android.mms.transaction.HttpUtils;
import com.android.mms.transaction.MmsMessageSender;
import com.android.mms.transaction.ProgressCallbackEntity;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.RateController;
import com.google.android.mms.APN;
import com.google.android.mms.APNHelper;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MMSPart;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduBody;
import com.google.android.mms.pdu_alt.PduComposer;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduPart;
import com.google.android.mms.pdu_alt.PduPersister;
import com.google.android.mms.pdu_alt.SendReq;
import com.google.android.mms.smil.SmilHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Class to process transaction requests for sending
 *
 * @author Jake Klinker
 */
public class Transaction {

    private static final String TAG = "Transaction";
    public static Settings mSettings;
    private Context mCtx;
    private ConnectivityManager mConnMgr;

    private boolean saveMessage = true;

    public String SMS_SENT = ".SMS_SENT";
    public String SMS_DELIVERED = ".SMS_DELIVERED";

    public static String NOTIFY_SMS_FAILURE = ".NOTIFY_SMS_FAILURE";
    public static final String MMS_ERROR = "com.klinker.android.send_message.MMS_ERROR";
    public static final String REFRESH = "com.klinker.android.send_message.REFRESH";
    public static final String MMS_PROGRESS = "com.klinker.android.send_message.MMS_PROGRESS";
    public static final String VOICE_FAILED = "com.klinker.android.send_message.VOICE_FAILED";
    public static final String VOICE_TOKEN = "com.klinker.android.send_message.RNRSE";
    public static final String NOTIFY_OF_DELIVERY = "com.klinker.android.send_message.NOTIFY_DELIVERY";
    public static final String NOTIFY_OF_MMS = "com.klinker.android.messaging.NEW_MMS_DOWNLOADED";

    public static final long NO_THREAD_ID = 0;

    // Modified by Orange
    public enum XmsEvent {
        SMS_RECEIVED, SMS_INSERT, SMS_SENT_OR_FAILED, SMS_DELIVERED_OR_FAILED, MMS_RECEIVED, MMS_INSERT, MMS_SENT_OR_FAILED
    }

    private static String sXmsEventIntentAction;

    public static final String XMS_EVENT = ".XMS_EVENT";

    public static void notifyXmsEvent(Context ctx, Uri uri, XmsEvent xmsEvent) {
        if (sXmsEventIntentAction == null) {
            String packageName = ctx.getPackageName();
            sXmsEventIntentAction = packageName.concat(XMS_EVENT);
        }
        Intent notify = new Intent(sXmsEventIntentAction);
        notify.putExtra("message_uri", uri);
        notify.putExtra("event", xmsEvent);
        ctx.sendBroadcast(notify);
    }

    /**
     * Sets context and initializes settings to default values
     *
     * @param context is the context of the activity or service
     */
    public Transaction(Context context) {
        this(context, new Settings());
    }

    /**
     * Sets context and settings
     *
     * @param context is the context of the activity or service
     * @param settings is the settings object to process send requests through
     */
    public Transaction(Context context, Settings settings) {
        mSettings = settings;
        mCtx = context;
        String packageName = mCtx.getPackageName();
        SMS_SENT = packageName.concat(SMS_SENT);
        SMS_DELIVERED = packageName.concat(SMS_DELIVERED);
        if (NOTIFY_SMS_FAILURE.equals(".NOTIFY_SMS_FAILURE")) {
            NOTIFY_SMS_FAILURE = packageName.concat(NOTIFY_SMS_FAILURE);
        }
    }

    /**
     * Called to send a new message depending on Settings and provided Message object If you want to
     * send message as mms, call this from the UI thread
     *
     * @param message is the message that you want to send
     * @param threadId is the thread id of who to send the message to (can also be set to
     *            Transaction.NO_THREAD_ID)
     */
    public void sendNewMessage(Message message, long threadId) {
        this.saveMessage = message.getSave();

        // if message:
        // 1) Has images attached
        // or
        // 1) is enabled to send long messages as mms
        // 2) number of pages for that sms exceeds value stored in Settings for when to send the mms
        // by
        // 3) prefer voice is disabled
        // or
        // 1) more than one address is attached
        // 2) group messaging is enabled
        //
        // then, send as MMS, else send as Voice or SMS
        if (checkMMS(message)) {
            try {
                Looper.prepare();
            } catch (Exception e) {
            }
            RateController.init(mCtx);
            DownloadManager.init(mCtx);
            sendMmsMessage(message.getText(), message.getAddresses(), message.getParts(),
                    message.getSubject());

        } else {
            if (message.getType() == Message.TYPE_VOICE) {
                sendVoiceMessage(message.getText(), message.getAddresses(), threadId);
            } else if (message.getType() == Message.TYPE_SMSMMS) {
                Log.v("send_transaction", "sending sms");
                sendSmsMessage(message.getText(), message.getAddresses(), threadId,
                        message.getDelay());
            } else {
                Log.v("send_transaction", "error with message type, aborting...");
            }
        }

    }

    private void sendSmsMessage(String text, String[] addresses, long threadId, int delay) {
        Log.v("send_transaction", "message text: " + text);
        Uri messageUri;
        int messageId = 0;
        if (saveMessage) {
            Log.v("send_transaction", "saving message");
            // add signature to original text to be saved in database (does not strip unicode for
            // saving though)
            if (!mSettings.getSignature().equals("")) {
                text += "\n" + mSettings.getSignature();
            }

            // save the message for each of the addresses
            for (int i = 0; i < addresses.length; i++) {
                Calendar cal = Calendar.getInstance();
                ContentValues values = new ContentValues();
                values.put(Telephony.TextBasedSmsColumns.ADDRESS, addresses[i]);
                values.put(Telephony.TextBasedSmsColumns.BODY,
                        mSettings.getStripUnicode() ? StripAccents.stripAccents(text) : text);
                values.put(Telephony.TextBasedSmsColumns.DATE, cal.getTimeInMillis() + "");
                values.put(Telephony.TextBasedSmsColumns.READ, 1);
                values.put(Telephony.TextBasedSmsColumns.TYPE,
                        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);
                values.put(Telephony.TextBasedSmsColumns.STATUS,
                        Telephony.TextBasedSmsColumns.STATUS_PENDING);

                // attempt to create correct thread id if one is not supplied
                if (threadId == NO_THREAD_ID || addresses.length > 1) {
                    threadId = Utils.getOrCreateThreadId(mCtx, addresses[i]);
                }

                Log.v("send_transaction", "saving message with thread id: " + threadId);
                values.put("thread_id", threadId);
                messageUri = mCtx.getContentResolver().insert(Uri.parse("content://sms/"), values);
                if (messageUri == null) {
                    Log.e("send_transaction", "Failed to insert SMS!");
                    continue;
                }
                notifyXmsEvent(mCtx, messageUri, XmsEvent.SMS_INSERT);

                Log.v("send_transaction", "inserted to uri: " + messageUri);

                Cursor query = mCtx.getContentResolver().query(messageUri, new String[] {
                    "_id"
                }, null, null, null);
                if (query != null && query.moveToFirst()) {
                    messageId = query.getInt(0);
                    query.close();
                }

                Log.v("send_transaction", "message id: " + messageId);

                // set up sent and delivered pending intents to be used with message request
                PendingIntent sentPI = PendingIntent.getBroadcast(mCtx, messageId, new Intent(
                        SMS_SENT).putExtra("message_uri", messageUri.toString()),
                        PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent deliveredPI = PendingIntent.getBroadcast(mCtx, messageId, new Intent(
                        SMS_DELIVERED).putExtra("message_uri", messageUri.toString()),
                        PendingIntent.FLAG_UPDATE_CURRENT);

                ArrayList<PendingIntent> sPI = new ArrayList<>();
                ArrayList<PendingIntent> dPI = new ArrayList<>();

                String body = text;

                // edit the body of the text if unicode needs to be stripped
                if (mSettings.getStripUnicode()) {
                    body = StripAccents.stripAccents(body);
                }

                if (!mSettings.getPreText().equals("")) {
                    body = mSettings.getPreText() + " " + body;
                }

                SmsManager smsManager = SmsManager.getDefault();
                Log.v("send_transaction", "found sms manager");

                if (mSettings.getSplit()) {
                    Log.v("send_transaction", "splitting message");
                    // figure out the length of supported message
                    int[] splitData = SmsMessage.calculateLength(body, false);

                    // we take the current length + the remaining length to get the total number of
                    // characters
                    // that message set can support, and then divide by the number of message that
                    // will require
                    // to get the length supported by a single message
                    int length = (body.length() + splitData[2]) / splitData[0];
                    Log.v("send_transaction", "length: " + length);

                    boolean counter = false;
                    if (mSettings.getSplitCounter() && body.length() > length) {
                        counter = true;
                        length -= 6;
                    }

                    // get the split messages
                    String[] textToSend = splitByLength(body, length, counter);

                    // send each message part to each recipient attached to message
                    for (String aTextToSend : textToSend) {
                        ArrayList<String> parts = smsManager.divideMessage(aTextToSend);

                        for (int k = 0; k < parts.size(); k++) {
                            sPI.add(saveMessage ? sentPI : null);
                            dPI.add(mSettings.getDeliveryReports() && saveMessage ? deliveredPI
                                    : null);
                        }

                        Log.v("send_transaction", "sending split message");
                        sendDelayedSms(smsManager, addresses[i], parts, sPI, dPI, delay, messageUri);
                    }
                } else {
                    Log.v("send_transaction", "sending without splitting");
                    // send the message normally without forcing anything to be split
                    ArrayList<String> parts = smsManager.divideMessage(body);

                    for (int j = 0; j < parts.size(); j++) {
                        sPI.add(saveMessage ? sentPI : null);
                        dPI.add(mSettings.getDeliveryReports() && saveMessage ? deliveredPI : null);
                    }

                    try {
                        Log.v("send_transaction", "sent message");
                        sendDelayedSms(smsManager, addresses[i], parts, sPI, dPI, delay, messageUri);
                    } catch (Exception e) {
                        // whoops...
                        Log.v("send_transaction", "error sending message");
                        Log.e(TAG, "exception thrown", e);

                        try {
                            ((Activity) mCtx).getWindow().getDecorView()
                                    .findViewById(android.R.id.content).post(new Runnable() {

                                        @Override
                                        public void run() {
                                            Toast.makeText(mCtx, "Message could not be sent",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } catch (Exception f) {
                        }
                    }
                }
            }
        }
    }

    private void sendDelayedSms(final SmsManager smsManager, final String address,
            final ArrayList<String> parts, final ArrayList<PendingIntent> sPI,
            final ArrayList<PendingIntent> dPI, final int delay, final Uri messageUri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                }

                if (checkIfMessageExistsAfterDelay(messageUri)) {
                    Log.v("send_transaction", "message sent after delay");
                    try {
                        smsManager.sendMultipartTextMessage(address, null, parts, sPI, dPI);
                    } catch (Exception e) {
                        Log.e(TAG, "exception thrown", e);
                    }
                } else {
                    Log.v("send_transaction", "message not sent after delay, no longer exists");
                }
            }
        }).start();
    }

    private boolean checkIfMessageExistsAfterDelay(Uri messageUti) {
        Cursor query = mCtx.getContentResolver().query(messageUti, new String[] {
            "_id"
        }, null, null, null);
        if (query != null && query.moveToFirst()) {
            query.close();
            return true;
        } else {
            return false;
        }
    }

    private void sendMmsMessage(String text, String[] addresses, Bitmap[] image,
            String[] imageNames, List<Message.Part> parts, String subject) {
        List<byte[]> images = new ArrayList<>();
        for (Bitmap bitmap : image) {
            images.add(Message.bitmapToByteArray(bitmap));
        }
        sendMmsMessage(text, addresses, parts, subject);
    }

    private void sendMmsMessage(String text, String[] addresses, List<Message.Part> parts,
            String subject) {
        // create the parts to send
        ArrayList<MMSPart> data = new ArrayList<>();
        // add any extra media according to their mimeType set in the message
        // eg. videos, audio, contact cards, location maybe?
        if (parts != null) {
            for (Message.Part p : parts) {
                MMSPart part = new MMSPart();
                part.Name = p.getName();
                part.MimeType = p.getContentType();
                part.Data = p.getMedia();
                data.add(part);
            }
        }
        if (!TextUtils.isEmpty(text)) {
            // add text to the end of the part and send
            MMSPart part = new MMSPart();
            part.Name = "text";
            part.MimeType = "text/plain";
            part.Data = text.getBytes();
            data.add(part);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            MessageInfo info = null;
            try {
                info = getBytes(mCtx, saveMessage, addresses,
                        data.toArray(new MMSPart[data.size()]), subject);

                // Modified by Orange
                final Uri messageUri = info.location;
                notifyXmsEvent(mCtx, messageUri, XmsEvent.MMS_INSERT);

                MmsMessageSender sender = new MmsMessageSender(mCtx, info.location,
                        info.bytes.length);
                sender.sendMessage(info.token);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ProgressCallbackEntity.PROGRESS_STATUS_ACTION);
                BroadcastReceiver receiver = new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int progress = intent.getIntExtra("progress", -3);
                        Log.v("sending_mms_library", "progress: " + progress);

                        // send progress broadcast to update ui if desired...
                        Intent progressIntent = new Intent(MMS_PROGRESS);
                        progressIntent.putExtra("progress", progress);
                        context.sendBroadcast(progressIntent);

                        if (progress == ProgressCallbackEntity.PROGRESS_COMPLETE) {

                            context.sendBroadcast(new Intent(REFRESH));

                            try {
                                context.unregisterReceiver(this);
                            } catch (Exception e) {
                                // TODO fix me
                                // receiver is not registered force close error... hmm.
                            }
                        } else if (progress == ProgressCallbackEntity.PROGRESS_ABORT) {
                            // This seems to get called only after the progress has reached 100 and
                            // then something else goes wrong, so here we will try and send again
                            // and see if it works
                            Log.v("sending_mms_library", "sending aborted for some reason...");
                        }
                    }

                };
                mCtx.registerReceiver(receiver, filter);

            } catch (Throwable e) {
                Log.e(TAG, "exception thrown", e);
                // insert the pdu into the database and return the bytes to send
                if (mSettings.getWifiMmsFix()) {
                    sendMMS(info.bytes);
                } else {
                    sendMMSWiFi(info.bytes);
                }
            }
        } else {
            Log.v(TAG, "using lollipop method for sending sms");

            if (mSettings.getUseSystemSending()) {
                Log.v(TAG, "using system method for sending");
                sendMmsThroughSystem(mCtx, subject, data, addresses);
            } else {
                try {
                    MessageInfo info = getBytes(mCtx, saveMessage, addresses,
                            data.toArray(new MMSPart[data.size()]), subject);
                    MmsRequestManager requestManager = new MmsRequestManager(mCtx, info.bytes);
                    SendRequest request = new SendRequest(requestManager,
                            Utils.getDefaultSubscriptionId(), info.location, null, null, null, null);
                    MmsNetworkManager manager = new MmsNetworkManager(mCtx,
                            Utils.getDefaultSubscriptionId());
                    request.execute(mCtx, manager);
                } catch (Exception e) {
                    Log.e(TAG, "error sending mms", e);
                }
            }
        }
    }

    public static MessageInfo getBytes(Context context, boolean saveMessage, String[] recipients,
            MMSPart[] parts, String subject) throws MmsException {
        final SendReq sendRequest = new SendReq();

        // create send request addresses
        for (int i = 0; i < recipients.length; i++) {
            final EncodedStringValue[] phoneNumbers = EncodedStringValue.extract(recipients[i]);

            if (phoneNumbers != null && phoneNumbers.length > 0) {
                sendRequest.addTo(phoneNumbers[0]);
            }
        }

        if (subject != null) {
            sendRequest.setSubject(new EncodedStringValue(subject));
        }

        sendRequest.setDate(Calendar.getInstance().getTimeInMillis() / 1000L);

        try {
            sendRequest.setFrom(new EncodedStringValue(Utils.getMyPhoneNumber(context)));
        } catch (Exception e) {
            Log.e(TAG, "error getting from address", e);
        }

        final PduBody pduBody = new PduBody();

        // assign parts to the pdu body which contains sending data
        long size = 0;
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                MMSPart part = parts[i];
                if (part != null) {
                    try {
                        PduPart partPdu = new PduPart();
                        partPdu.setName(part.Name.getBytes());
                        partPdu.setContentLocation(part.Name.getBytes());
                        partPdu.setContentType(part.MimeType.getBytes());

                        if (part.MimeType.startsWith("text")) {
                            partPdu.setCharset(CharacterSets.UTF_8);
                        }

                        partPdu.setData(part.Data);

                        pduBody.addPart(partPdu);
                        size += (part.Name.getBytes().length + part.MimeType.getBytes().length + part.Data.length);
                    } catch (Exception e) {
                    }
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(SmilHelper.createSmilDocument(pduBody), out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        pduBody.addPart(0, smilPart);

        sendRequest.setBody(pduBody);

        Log.v(TAG, "setting message size to " + size + " bytes");
        sendRequest.setMessageSize(size);

        // add everything else that could be set
        sendRequest.setPriority(PduHeaders.PRIORITY_NORMAL);
        sendRequest.setDeliveryReport(PduHeaders.VALUE_NO);
        sendRequest.setExpiry(1000 * 60 * 60 * 24 * 7);
        sendRequest.setMessageClass(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes());
        sendRequest.setReadReport(PduHeaders.VALUE_NO);

        // create byte array which will actually be sent
        final PduComposer composer = new PduComposer(context, sendRequest);
        final byte[] bytesToSend;

        try {
            bytesToSend = composer.make();
        } catch (OutOfMemoryError e) {
            throw new MmsException("Out of memory!");
        }

        MessageInfo info = new MessageInfo();
        info.bytes = bytesToSend;

        if (saveMessage) {
            try {
                PduPersister persister = PduPersister.getPduPersister(context);
                info.location = persister.persist(sendRequest, Uri.parse("content://mms/outbox"),
                        true, mSettings.getGroup(), null);
            } catch (Exception e) {
                Log.v("sending_mms_library", "error saving mms message");
                Log.e(TAG, "exception thrown", e);

                // use the old way if something goes wrong with the persister
                insert(context, recipients, parts, subject);
            }
        }

        try {
            Cursor query = context.getContentResolver().query(info.location, new String[] {
                "thread_id"
            }, null, null, null);
            if (query != null && query.moveToFirst()) {
                info.token = query.getLong(query.getColumnIndex("thread_id"));
                query.close();
            } else {
                // just default sending token for what I had before
                info.token = 4444L;
            }
        } catch (Exception e) {
            Log.e(TAG, "exception thrown", e);
            info.token = 4444L;
        }

        return info;
    }

    public static final long DEFAULT_EXPIRY_TIME = 7 * 24 * 60 * 60;
    public static final int DEFAULT_PRIORITY = PduHeaders.PRIORITY_NORMAL;

    private static void sendMmsThroughSystem(Context context, String subject, List<MMSPart> parts,
            String[] addresses) {
        try {
            final String fileName = "send." + String.valueOf(Math.abs(new Random().nextLong()))
                    + ".dat";
            File mSendFile = new File(context.getCacheDir(), fileName);

            SendReq sendReq = buildPdu(context, addresses, subject, parts);
            PduPersister persister = PduPersister.getPduPersister(context);
            Uri messageUri = persister.persist(sendReq, Uri.parse("content://mms/outbox"), true,
                    mSettings.getGroup(), null);

            notifyXmsEvent(context, messageUri, XmsEvent.MMS_INSERT);

            Intent intent = new Intent(MmsSentReceiver.MMS_SENT);
            intent.putExtra(MmsSentReceiver.EXTRA_CONTENT_URI, messageUri.toString());
            intent.putExtra(MmsSentReceiver.EXTRA_FILE_PATH, mSendFile.getPath());
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            Uri writerUri = (new Uri.Builder())
                    .authority(context.getPackageName() + ".MmsFileProvider").path(fileName)
                    .scheme(ContentResolver.SCHEME_CONTENT).build();
            FileOutputStream writer = null;
            Uri contentUri = null;
            try {
                writer = new FileOutputStream(mSendFile);
                writer.write(new PduComposer(context, sendReq).make());
                contentUri = writerUri;
            } catch (final IOException e) {
                Log.e(TAG, "Error writing send file", e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                    }
                }
            }

            Bundle configOverrides = new Bundle();
            configOverrides.putBoolean(SmsManager.MMS_CONFIG_GROUP_MMS_ENABLED,
                    mSettings.getGroup());

            if (contentUri != null) {
                SmsManager.getDefault().sendMultimediaMessage(context, contentUri, null,
                        configOverrides, pendingIntent);
            } else {
                Log.e(TAG, "Error writing sending Mms");
                try {
                    pendingIntent.send(SmsManager.MMS_ERROR_IO_ERROR);
                } catch (PendingIntent.CanceledException ex) {
                    Log.e(TAG, "Mms pending intent cancelled?", ex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error using system sending method", e);
        }
    }

    private static SendReq buildPdu(Context context, String[] recipients, String subject,
            List<MMSPart> parts) {
        final SendReq req = new SendReq();
        // From, per spec
        final String lineNumber = Utils.getMyPhoneNumber(context);
        if (!TextUtils.isEmpty(lineNumber)) {
            req.setFrom(new EncodedStringValue(lineNumber));
        }
        // To
        for (String recipient : recipients) {
            req.addTo(new EncodedStringValue(recipient));
        }
        // Subject
        if (!TextUtils.isEmpty(subject)) {
            req.setSubject(new EncodedStringValue(subject));
        }
        // Date
        req.setDate(System.currentTimeMillis() / 1000);
        // Body
        PduBody body = new PduBody();
        // Add text part. Always add a smil part for compatibility, without it there
        // may be issues on some carriers/client apps
        int size = 0;
        for (int i = 0; i < parts.size(); i++) {
            MMSPart part = parts.get(i);
            size += addTextPart(body, part, i);
        }

        // add a SMIL document for compatibility
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(SmilHelper.createSmilDocument(body), out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        body.addPart(0, smilPart);

        req.setBody(body);
        // Message size
        req.setMessageSize(size);
        // Message class
        req.setMessageClass(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes());
        // Expiry
        req.setExpiry(DEFAULT_EXPIRY_TIME);
        try {
            // Priority
            req.setPriority(DEFAULT_PRIORITY);
            // Delivery report
            req.setDeliveryReport(PduHeaders.VALUE_NO);
            // Read report
            req.setReadReport(PduHeaders.VALUE_NO);
        } catch (InvalidHeaderValueException e) {
        }

        return req;
    }

    private static int addTextPart(PduBody pb, MMSPart p, int id) {
        String filename = p.MimeType.split("/")[0] + "_" + id + ".mms";
        final PduPart part = new PduPart();
        // Set Content-Type.
        part.setContentType(p.MimeType.getBytes());
        // Set Charset if it's a text media.
        if (p.MimeType.startsWith("text")) {
            part.setCharset(CharacterSets.UTF_8);
            // Set Content-Location.
            part.setContentLocation(filename.getBytes());
            int index = filename.lastIndexOf(".");
            String contentId = (index == -1) ? filename : filename.substring(0, index);
            part.setContentId(contentId.getBytes());

        } else {
            // Set Content-Location.
            part.setContentLocation(p.Name.getBytes());
            part.setContentId(p.Name.getBytes());
        }
        part.setData(p.Data);
        pb.addPart(part);

        return part.getData().length;
    }

    public static class MessageInfo {
        public long token;
        public Uri location;
        public byte[] bytes;
    }

    private void sendVoiceMessage(String text, String[] addresses, long threadId) {
        // send a voice message to each recipient based off of koush's voice implementation in
        // Voice+
        for (int i = 0; i < addresses.length; i++) {
            if (saveMessage) {
                Calendar cal = Calendar.getInstance();
                ContentValues values = new ContentValues();
                values.put("address", addresses[i]);
                values.put("body", text);
                values.put("date", cal.getTimeInMillis() + "");
                values.put("read", 1);
                values.put("status", 2); // if you want to be able to tell the difference between
                // sms and voice, look for this value. SMS will be -1, 0,
                // 64, 128 and voice will be 2

                // attempt to create correct thread id if one is not supplied
                if (threadId == NO_THREAD_ID || addresses.length > 1) {
                    threadId = Utils.getOrCreateThreadId(mCtx, addresses[i]);
                }

                values.put("thread_id", threadId);
                mCtx.getContentResolver().insert(Uri.parse("content://sms/outbox"), values);
            }

            if (!mSettings.getSignature().equals("")) {
                text += "\n" + mSettings.getSignature();
            }

            sendVoiceMessage(addresses[i], text);
        }
    }

    // splits text and adds split counter when applicable
    private String[] splitByLength(String s, int chunkSize, boolean counter) {
        int arraySize = (int) Math.ceil((double) s.length() / chunkSize);

        String[] returnArray = new String[arraySize];

        int index = 0;
        for (int i = 0; i < s.length(); i = i + chunkSize) {
            if (s.length() - i < chunkSize) {
                returnArray[index++] = s.substring(i);
            } else {
                returnArray[index++] = s.substring(i, i + chunkSize);
            }
        }

        if (counter && returnArray.length > 1) {
            for (int i = 0; i < returnArray.length; i++) {
                returnArray[i] = "(" + (i + 1) + "/" + returnArray.length + ") " + returnArray[i];
            }
        }

        return returnArray;
    }

    private boolean alreadySending = false;

    private void sendMMS(final byte[] bytesToSend) {
        revokeWifi(true);

        // enable mms connection to mobile data
        mConnMgr = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        int result = beginMmsConnectivity();

        Log.v("sending_mms_library", "result of connectivity: " + result + " ");

        if (result != 0) {
            // if mms feature is not already running (most likely isn't...) then register a receiver
            // and wait for it to be active
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            final BroadcastReceiver receiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context1, Intent intent) {
                    String action = intent.getAction();

                    if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        return;
                    }

                    @SuppressWarnings("deprecation")
                    NetworkInfo mNetworkInfo = intent
                            .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

                    if ((mNetworkInfo == null)
                            || (mNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                        return;
                    }

                    if (!mNetworkInfo.isConnected()) {
                        return;
                    } else {
                        // ready to send the message now
                        Log.v("sending_mms_library", "sending through broadcast receiver");
                        alreadySending = true;
                        sendData(bytesToSend);

                        mCtx.unregisterReceiver(this);
                    }

                }

            };

            mCtx.registerReceiver(receiver, filter);

            try {
                Looper.prepare();
            } catch (Exception e) {
                // Already on UI thread probably
            }

            // try sending after 3 seconds anyways if for some reason the receiver doesn't work
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!alreadySending) {
                        try {
                            Log.v("sending_mms_library", "sending through handler");
                            mCtx.unregisterReceiver(receiver);
                        } catch (Exception e) {

                        }

                        sendData(bytesToSend);
                    }
                }
            }, 7000);
        } else {
            // mms connection already active, so send the message
            Log.v("sending_mms_library", "sending right away, already ready");
            sendData(bytesToSend);
        }
    }

    private void sendMMSWiFi(final byte[] bytesToSend) {
        // enable mms connection to mobile data
        mConnMgr = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo.State state = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS)
                .getState();

        if ((0 == state.compareTo(NetworkInfo.State.CONNECTED) || 0 == state
                .compareTo(NetworkInfo.State.CONNECTING))) {
            sendData(bytesToSend);
        } else {
            int resultInt = mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                    "enableMMS");

            if (resultInt == 0) {
                try {
                    Utils.ensureRouteToHost(mCtx, mSettings.getMmsc(), mSettings.getProxy());
                    sendData(bytesToSend);
                } catch (Exception e) {
                    Log.e(TAG, "exception thrown", e);
                    sendData(bytesToSend);
                }
            } else {
                // if mms feature is not already running (most likely isn't...) then register a
                // receiver and wait for it to be active
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                final BroadcastReceiver receiver = new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context context1, Intent intent) {
                        String action = intent.getAction();

                        if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                            return;
                        }

                        NetworkInfo mNetworkInfo = mConnMgr.getActiveNetworkInfo();
                        if ((mNetworkInfo == null)
                                || (mNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE_MMS)) {
                            return;
                        }

                        if (!mNetworkInfo.isConnected()) {
                            return;
                        } else {
                            alreadySending = true;

                            try {
                                Utils.ensureRouteToHost(mCtx, mSettings.getMmsc(),
                                        mSettings.getProxy());
                                sendData(bytesToSend);
                            } catch (Exception e) {
                                Log.e(TAG, "exception thrown", e);
                                sendData(bytesToSend);
                            }

                            mCtx.unregisterReceiver(this);
                        }

                    }

                };

                mCtx.registerReceiver(receiver, filter);

                try {
                    Looper.prepare();
                } catch (Exception e) {
                    // Already on UI thread probably
                }

                // try sending after 3 seconds anyways if for some reason the receiver doesn't work
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!alreadySending) {
                            try {
                                mCtx.unregisterReceiver(receiver);
                            } catch (Exception e) {

                            }

                            try {
                                Utils.ensureRouteToHost(mCtx, mSettings.getMmsc(),
                                        mSettings.getProxy());
                                sendData(bytesToSend);
                            } catch (Exception e) {
                                Log.e(TAG, "exception thrown", e);
                                sendData(bytesToSend);
                            }
                        }
                    }
                }, 7000);
            }
        }
    }

    private void sendData(final byte[] bytesToSend) {
        // be sure this is running on new thread, not UI
        Log.v("sending_mms_library", "starting new thread to send on");
        new Thread(new Runnable() {

            @Override
            public void run() {
                List<APN> apns = new ArrayList<APN>();

                try {
                    APN apn = new APN(mSettings.getMmsc(), mSettings.getPort(), mSettings
                            .getProxy());
                    apns.add(apn);

                    String mmscUrl = apns.get(0).MMSCenterUrl != null ? apns.get(0).MMSCenterUrl
                            .trim() : null;
                    apns.get(0).MMSCenterUrl = mmscUrl;

                    if (apns.get(0).MMSCenterUrl.equals("")) {
                        // attempt to get apns from internal databases, most likely will fail due to
                        // insignificant permissions
                        APNHelper helper = new APNHelper(mCtx);
                        apns = helper.getMMSApns();
                    }
                } catch (Exception e) {
                    // error in the apns, none are available most likely causing an index out of
                    // bounds
                    // exception. cant send a message, so therefore mark as failed
                    markMmsFailed();
                    return;
                }

                try {
                    // attempts to send the message using given apns
                    Log.v("sending_mms_library", apns.get(0).MMSCenterUrl + " "
                            + apns.get(0).MMSProxy + " " + apns.get(0).MMSPort);
                    Log.v("sending_mms_libarry", "initial attempt at sending starting now");
                    trySending(apns.get(0), bytesToSend, 0);
                } catch (Exception e) {
                    // some type of apn error, so notify user of failure
                    Log.v("sending_mms_libary",
                            "weird error, not sure how this could even be called other than apn stuff");
                    markMmsFailed();
                }

            }

        }).start();
    }

    public static final int NUM_RETRIES = 2;

    private void trySending(final APN apns, final byte[] bytesToSend, final int numRetries) {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ProgressCallbackEntity.PROGRESS_STATUS_ACTION);
            BroadcastReceiver receiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    int progress = intent.getIntExtra("progress", -3);
                    Log.v("sending_mms_library", "progress: " + progress);

                    // send progress broadcast to update ui if desired...
                    Intent progressIntent = new Intent(MMS_PROGRESS);
                    progressIntent.putExtra("progress", progress);
                    context.sendBroadcast(progressIntent);

                    if (progress == ProgressCallbackEntity.PROGRESS_COMPLETE) {
                        if (saveMessage) {
                            Cursor query = context.getContentResolver().query(
                                    Uri.parse("content://mms"), new String[] {
                                        "_id"
                                    }, null, null, "date desc");
                            if (query != null && query.moveToFirst()) {
                                String id = query.getString(query.getColumnIndex("_id"));
                                query.close();

                                // move to the sent box
                                ContentValues values = new ContentValues();
                                values.put("msg_box", 2);
                                String where = "_id" + " = '" + id + "'";
                                context.getContentResolver().update(Uri.parse("content://mms"),
                                        values, where, null);
                            }
                        }

                        context.sendBroadcast(new Intent(REFRESH));

                        try {
                            context.unregisterReceiver(this);
                        } catch (Exception e) { /* Receiver not registered */
                        }

                        // give everything time to finish up, may help the abort being shown after
                        // the progress is already 100
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mConnMgr.stopUsingNetworkFeature(
                                        ConnectivityManager.TYPE_MOBILE_MMS, "enableMMS");
                                if (mSettings.getWifiMmsFix()) {
                                    reinstateWifi();
                                }
                            }
                        }, 1000);
                    } else if (progress == ProgressCallbackEntity.PROGRESS_ABORT) {
                        // This seems to get called only after the progress has reached 100 and then
                        // something else goes wrong, so here we will try and send again and see if
                        // it works
                        Log.v("sending_mms_library", "sending aborted for some reason...");
                        context.unregisterReceiver(this);

                        if (numRetries < NUM_RETRIES) {
                            // sleep and try again in three seconds to see if that give wifi and
                            // mobile data a chance to toggle in time
                            try {
                                Thread.sleep(3000);
                            } catch (Exception f) {

                            }

                            if (mSettings.getWifiMmsFix()) {
                                sendMMS(bytesToSend);
                            } else {
                                sendMMSWiFi(bytesToSend);
                            }
                        } else {
                            markMmsFailed();
                        }
                    }
                }

            };

            mCtx.registerReceiver(receiver, filter);

            // This is where the actual post request is made to send the bytes we previously created
            // through the given apns
            Log.v("sending_mms_library", "attempt: " + numRetries);
            Utils.ensureRouteToHost(mCtx, apns.MMSCenterUrl, apns.MMSProxy);
            HttpUtils.httpConnection(mCtx, 4444L, apns.MMSCenterUrl, bytesToSend,
                    HttpUtils.HTTP_POST_METHOD, !TextUtils.isEmpty(apns.MMSProxy), apns.MMSProxy,
                    Integer.parseInt(apns.MMSPort));
        } catch (IOException e) {
            Log.v("sending_mms_library", "some type of error happened when actually sending maybe?");
            Log.e(TAG, "exception thrown", e);

            if (numRetries < NUM_RETRIES) {
                // sleep and try again in three seconds to see if that give wifi and mobile data a
                // chance to toggle in time
                try {
                    Thread.sleep(3000);
                } catch (Exception f) {

                }

                trySending(apns, bytesToSend, numRetries + 1);
            } else {
                markMmsFailed();
            }
        }
    }

    private void markMmsFailed() {
        // if it still fails, then mark message as failed
        if (mSettings.getWifiMmsFix()) {
            reinstateWifi();
        }

        if (saveMessage) {
            Cursor query = mCtx.getContentResolver().query(Uri.parse("content://mms"),
                    new String[] {
                        "_id"
                    }, null, null, "date desc");
            if (query != null && query.moveToFirst()) {
                String id = query.getString(query.getColumnIndex("_id"));
                query.close();

                // mark message as failed
                ContentValues values = new ContentValues();
                values.put("msg_box", 5);
                String where = "_id" + " = '" + id + "'";
                mCtx.getContentResolver().update(Uri.parse("content://mms"), values, where, null);
            }
        }

        ((Activity) mCtx).getWindow().getDecorView().findViewById(android.R.id.content)
                .post(new Runnable() {

                    @Override
                    public void run() {
                        mCtx.sendBroadcast(new Intent(REFRESH));
                        mCtx.sendBroadcast(new Intent(NOTIFY_SMS_FAILURE));

                        // broadcast that mms has failed and you can notify user from there if you
                        // would like
                        mCtx.sendBroadcast(new Intent(MMS_ERROR));

                    }

                });
    }

    private void sendVoiceMessage(final String destAddr, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String rnrse = mSettings.getRnrSe();
                String account = mSettings.getAccount();
                String authToken;

                try {
                    authToken = Utils.getAuthToken(account, mCtx);

                    if (rnrse == null) {
                        rnrse = fetchRnrSe(authToken, mCtx);
                    }
                } catch (Exception e) {
                    failVoice();
                    return;
                }

                try {
                    sendRnrSe(authToken, rnrse, destAddr, text);
                    successVoice();
                    return;
                } catch (Exception e) {
                }

                try {
                    // try again...
                    rnrse = fetchRnrSe(authToken, mCtx);
                    sendRnrSe(authToken, rnrse, destAddr, text);
                    successVoice();
                } catch (Exception e) {
                    failVoice();
                }
            }
        }).start();
    }

    // hit the google voice api to send a text
    private void sendRnrSe(String authToken, String rnrse, String number, String text)
            throws Exception {
        JsonObject json = Ion.with(mCtx).load("https://www.google.com/voice/sms/send/")
                .setHeader("Authorization", "GoogleLogin auth=" + authToken)
                .setBodyParameter("phoneNumber", number).setBodyParameter("sendErrorSms", "0")
                .setBodyParameter("text", text).setBodyParameter("_rnr_se", rnrse).asJsonObject()
                .get();

        if (!json.get("ok").getAsBoolean())
            throw new Exception(json.toString());
    }

    private void failVoice() {
        if (saveMessage) {
            Cursor query = mCtx.getContentResolver().query(Uri.parse("content://sms/outbox"), null,
                    null, null, null);

            // mark message as failed
            if (query != null && query.moveToFirst()) {
                String id = query.getString(query.getColumnIndex("_id"));
                ContentValues values = new ContentValues();
                values.put("type", "5");
                values.put("read", true);
                mCtx.getContentResolver().update(Uri.parse("content://sms/outbox"), values,
                        "_id=" + id, null);

                query.close();
            }
        }

        mCtx.sendBroadcast(new Intent(REFRESH));
        mCtx.sendBroadcast(new Intent(VOICE_FAILED));
    }

    private void successVoice() {
        if (saveMessage) {
            Cursor query = mCtx.getContentResolver().query(Uri.parse("content://sms/outbox"), null,
                    null, null, null);

            // mark message as sent successfully
            if (query.moveToFirst()) {
                String id = query.getString(query.getColumnIndex("_id"));
                ContentValues values = new ContentValues();
                values.put("type", "2");
                values.put("read", true);
                mCtx.getContentResolver().update(Uri.parse("content://sms/outbox"), values,
                        "_id=" + id, null);
            }

            query.close();
        }

        mCtx.sendBroadcast(new Intent(REFRESH));
    }

    private String fetchRnrSe(String authToken, Context context) throws ExecutionException,
            InterruptedException {
        JsonObject userInfo = Ion.with(context).load("https://www.google.com/voice/request/user")
                .setHeader("Authorization", "GoogleLogin auth=" + authToken).asJsonObject().get();

        String rnrse = userInfo.get("r").getAsString();

        try {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Activity.TELEPHONY_SERVICE);
            String number = tm.getLine1Number();
            if (number != null) {
                JsonObject phones = userInfo.getAsJsonObject("phones");
                for (Map.Entry<String, JsonElement> entry : phones.entrySet()) {
                    JsonObject phone = entry.getValue().getAsJsonObject();
                    if (!PhoneNumberUtils.compare(number, phone.get("phoneNumber").getAsString()))
                        continue;
                    if (!phone.get("smsEnabled").getAsBoolean())
                        break;

                    Ion.with(context)
                            .load("https://www.google.com/voice/mSettings/editForwardingSms/")
                            .setHeader("Authorization", "GoogleLogin auth=" + authToken)
                            .setBodyParameter("phoneId", entry.getKey())
                            .setBodyParameter("enabled", "0").setBodyParameter("_rnr_se", rnrse)
                            .asJsonObject();
                    break;
                }
            }
        } catch (Exception e) {

        }

        // broadcast so you can save it to your shared prefs or something so that it doesn't need to
        // be retrieved every time
        Intent intent = new Intent(VOICE_TOKEN);
        intent.putExtra("_rnr_se", rnrse);
        context.sendBroadcast(intent);

        return rnrse;
    }

    private static Uri insert(Context context, String[] to, MMSPart[] parts, String subject) {
        try {
            Uri destUri = Uri.parse("content://mms");

            Set<String> recipients = new HashSet<String>();
            recipients.addAll(Arrays.asList(to));
            long thread_id = Utils.getOrCreateThreadId(context, recipients);

            // Create a dummy sms
            ContentValues dummyValues = new ContentValues();
            dummyValues.put("thread_id", thread_id);
            dummyValues.put("body", " ");
            Uri dummySms = context.getContentResolver().insert(Uri.parse("content://sms/sent"),
                    dummyValues);

            // Create a new message entry
            long now = System.currentTimeMillis();
            ContentValues mmsValues = new ContentValues();
            mmsValues.put("thread_id", thread_id);
            mmsValues.put("date", now / 1000L);
            mmsValues.put("msg_box", 4);
            mmsValues.put("m_id", System.currentTimeMillis());
            mmsValues.put("read", true);
            mmsValues.put("sub", subject != null ? subject : "");
            mmsValues.put("sub_cs", 106);
            mmsValues.put("ct_t", "application/vnd.wap.multipart.related");

            long imageBytes = 0;

            for (MMSPart part : parts) {
                imageBytes += part.Data.length;
            }

            mmsValues.put("exp", imageBytes);

            mmsValues.put("m_cls", "personal");
            mmsValues.put("m_type", 128); // 132 (RETRIEVE CONF) 130 (NOTIF IND) 128 (SEND REQ)
            mmsValues.put("v", 19);
            mmsValues.put("pri", 129);
            mmsValues.put("tr_id", "T" + Long.toHexString(now));
            mmsValues.put("resp_st", 128);

            // Insert message
            Uri res = context.getContentResolver().insert(destUri, mmsValues);
            String messageId = res.getLastPathSegment().trim();

            // Create part
            for (MMSPart part : parts) {
                if (part.MimeType.startsWith("image")) {
                    createPartImage(context, messageId, part.Data, part.MimeType);
                } else if (part.MimeType.startsWith("text")) {
                    createPartText(context, messageId, new String(part.Data, "UTF-8"));
                }
            }

            // Create addresses
            for (String addr : to) {
                createAddr(context, messageId, addr);
            }

            // res = Uri.parse(destUri + "/" + messageId);

            // Delete dummy sms
            context.getContentResolver().delete(dummySms, null, null);

            return res;
        } catch (Exception e) {
            Log.v("sending_mms_library", "still an error saving... :(");
            Log.e(TAG, "exception thrown", e);
        }

        return null;
    }

    // create the image part to be stored in database
    private static Uri createPartImage(Context context, String id, byte[] imageBytes,
            String mimeType) throws Exception {
        ContentValues mmsPartValue = new ContentValues();
        mmsPartValue.put("mid", id);
        mmsPartValue.put("ct", mimeType);
        mmsPartValue.put("cid", "<" + System.currentTimeMillis() + ">");
        Uri partUri = Uri.parse("content://mms/" + id + "/part");
        Uri res = context.getContentResolver().insert(partUri, mmsPartValue);

        // Add data to part
        OutputStream os = context.getContentResolver().openOutputStream(res);
        ByteArrayInputStream is = new ByteArrayInputStream(imageBytes);
        byte[] buffer = new byte[256];

        for (int len = 0; (len = is.read(buffer)) != -1;) {
            os.write(buffer, 0, len);
        }

        os.close();
        is.close();

        return res;
    }

    // create the text part to be stored in database
    private static Uri createPartText(Context context, String id, String text) throws Exception {
        ContentValues mmsPartValue = new ContentValues();
        mmsPartValue.put("mid", id);
        mmsPartValue.put("ct", "text/plain");
        mmsPartValue.put("cid", "<" + System.currentTimeMillis() + ">");
        mmsPartValue.put("text", text);
        Uri partUri = Uri.parse("content://mms/" + id + "/part");
        Uri res = context.getContentResolver().insert(partUri, mmsPartValue);

        return res;
    }

    // add address to the request
    private static Uri createAddr(Context context, String id, String addr) throws Exception {
        ContentValues addrValues = new ContentValues();
        addrValues.put("address", addr);
        addrValues.put("charset", "106");
        addrValues.put("type", 151); // TO
        Uri addrUri = Uri.parse("content://mms/" + id + "/addr");
        Uri res = context.getContentResolver().insert(addrUri, addrValues);

        return res;
    }

    /**
     * A method for checking whether or not a certain message will be sent as mms depending on its
     * contents and the mSettings
     *
     * @param message is the message that you are checking against
     * @return true if the message will be mms, otherwise false
     */
    public boolean checkMMS(Message message) {
        return (message.getParts() != null && message.getParts().size() != 0)
                || (mSettings.getSendLongAsMms()
                        && Utils.getNumPages(mSettings, message.getText()) > mSettings
                                .getSendLongAsMmsAfter() && message.getType() != Message.TYPE_VOICE)
                || (message.getAddresses().length > 1 && mSettings.getGroup())
                || message.getSubject() != null;
    }

    /**
     * @deprecated
     */
    private void reinstateWifi() {
        try {
            mCtx.unregisterReceiver(mSettings.discon);
        } catch (Exception f) {

        }

        WifiManager wifi = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(false);
        wifi.setWifiEnabled(mSettings.currentWifiState);
        wifi.reconnect();
        Utils.setMobileDataEnabled(mCtx, mSettings.currentDataState);
    }

    /**
     * @deprecated
     */
    private void revokeWifi(boolean saveState) {
        WifiManager wifi = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);

        if (saveState) {
            mSettings.currentWifi = wifi.getConnectionInfo();
            mSettings.currentWifiState = wifi.isWifiEnabled();
            wifi.disconnect();
            mSettings.discon = new DisconnectWifi();
            mCtx.registerReceiver(mSettings.discon, new IntentFilter(
                    WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
            mSettings.currentDataState = Utils.isMobileDataEnabled(mCtx);
            Utils.setMobileDataEnabled(mCtx, true);
        } else {
            wifi.disconnect();
            wifi.disconnect();
            mSettings.discon = new DisconnectWifi();
            mCtx.registerReceiver(mSettings.discon, new IntentFilter(
                    WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
            Utils.setMobileDataEnabled(mCtx, true);
        }
    }

    /**
     * @deprecated
     */
    private int beginMmsConnectivity() {
        Log.v("sending_mms_library", "starting mms service");
        return mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableMMS");
    }
}
