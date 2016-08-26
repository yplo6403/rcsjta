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

package com.gsma.rcs.core.cms.xms;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.cms.CmsData;
import com.gsma.rcs.provider.cms.CmsData.DeleteStatus;
import com.gsma.rcs.provider.cms.CmsData.MessageType;
import com.gsma.rcs.provider.cms.CmsData.PushStatus;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.provider.cms.CmsXmsObject;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.smsmms.MmsLog;
import com.gsma.rcs.provider.smsmms.SmsLog;
import com.gsma.rcs.provider.smsmms.SmsMmsLog;
import com.gsma.rcs.provider.xms.XmsLog;
import com.gsma.rcs.provider.xms.model.MmsDataObject;
import com.gsma.rcs.provider.xms.model.SmsDataObject;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.ReadStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.TextBasedSmsColumns;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Synchronizes the native content provider with the RCS XMS local content provider.<br>
 * Synchronization is only performed once upon start up.
 */
public class XmsSynchronizer {

    private static final Logger sLogger = Logger.getLogger(XmsSynchronizer.class.getName());

    // @formatter:off
    private static final String[] PROJECTION_SMS = new String[]{
            BaseColumns._ID,
            TextBasedSmsColumns.THREAD_ID,
            TextBasedSmsColumns.ADDRESS,
            TextBasedSmsColumns.DATE,
            TextBasedSmsColumns.DATE_SENT,
            TextBasedSmsColumns.PROTOCOL,
            TextBasedSmsColumns.BODY,
            TextBasedSmsColumns.READ,
            TextBasedSmsColumns.TYPE,
            TextBasedSmsColumns.STATUS,
            TextBasedSmsColumns.ERROR_CODE
    };
    // @formatter:on

    private static final String[] PROJECTION_ID_READ = new String[] {
            BaseColumns._ID, TextBasedSmsColumns.READ
    };

    private static final String SELECTION_CONTACT_NOT_NULL = TextBasedSmsColumns.ADDRESS
            + " IS NOT NULL";
    static final String SELECTION_BASE_ID = BaseColumns._ID + "=?" + " AND "
            + SELECTION_CONTACT_NOT_NULL;

    private static final String WHERE_MSG_ID_NOT_NULL = Telephony.Mms.MESSAGE_ID + " is not null";

    private final ContentResolver mContentResolver;
    private final XmsLog mXmsLog;
    private final CmsLog mCmsLog;
    private final RcsSettings mSettings;
    private final SmsMmsLog mSmsMmsLog;

    private Set<Long> mNativeIds;
    private Set<Long> mNativeReadIds;

    public XmsSynchronizer(ContentResolver resolver, RcsSettings settings, XmsLog xmsLog,
            CmsLog cmsLog, SmsMmsLog smsMmsLog) {
        mContentResolver = resolver;
        mXmsLog = xmsLog;
        mCmsLog = cmsLog;
        mSettings = settings;
        mSmsMmsLog = smsMmsLog;
    }

    private void syncSms() throws FileAccessException {
        updateSetOfNativeSmsIds();
        Map<Long, CmsXmsObject> rcsMessages = mCmsLog.getNativeMessages(MessageType.SMS);
        checkDeletedMessages(MessageType.SMS, rcsMessages);
        checkNewMessages(MessageType.SMS, rcsMessages);
        checkReadMessages(MessageType.SMS, rcsMessages);
    }

    private void syncMms() throws FileAccessException {
        updateSetOfNativeMmsIds();
        Map<Long, CmsXmsObject> rcsMessages = mCmsLog.getNativeMessages(MessageType.MMS);
        checkDeletedMessages(MessageType.MMS, rcsMessages);
        checkNewMessages(MessageType.MMS, rcsMessages);
        checkReadMessages(MessageType.MMS, rcsMessages);
    }

    private void updateSetOfNativeSmsIds() {
        mNativeIds = new HashSet<>();
        mNativeReadIds = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(SmsLog.Sms.URI, PROJECTION_ID_READ, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, SmsLog.Sms.URI);
            int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            int readIdx = cursor.getColumnIndexOrThrow(TextBasedSmsColumns.READ);
            while (cursor.moveToNext()) {
                Long id = cursor.getLong(idIdx);
                mNativeIds.add(id);
                if (cursor.getInt(readIdx) == 1) {
                    mNativeReadIds.add(id);
                }
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private void updateSetOfNativeMmsIds() {
        Cursor cursor = null;
        mNativeIds = new HashSet<>();
        mNativeReadIds = new HashSet<>();
        try {
            cursor = mContentResolver.query(MmsLog.Mms.Pdu.URI, PROJECTION_ID_READ,
                    WHERE_MSG_ID_NOT_NULL, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, MmsLog.Mms.Pdu.URI);
            int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            int readIdx = cursor.getColumnIndexOrThrow(TextBasedSmsColumns.READ);
            while (cursor.moveToNext()) {
                Long id = cursor.getLong(idIdx);
                mNativeIds.add(id);
                if (cursor.getInt(readIdx) == 1) {
                    mNativeReadIds.add(id);
                }
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    private void purgeDeletedMessages() {
        int nb = mCmsLog.purgeDeletedMessages();
        if (sLogger.isActivated()) {
            sLogger.debug(nb + " messages have been removed from Imap data");
        }
    }

    private void checkDeletedMessages(MessageType messageType, Map<Long, CmsXmsObject> rcsMessages) {
        if (rcsMessages.isEmpty()) {
            return;
        }
        Set<Long> deletedIds = new HashSet<>(rcsMessages.keySet());
        deletedIds.removeAll(mNativeIds);
        for (Long id : deletedIds) {
            CmsObject cmsObject = rcsMessages.get(id);
            DeleteStatus deleteStatus = cmsObject.getDeleteStatus();
            if (DeleteStatus.NOT_DELETED == deleteStatus) {
                if (sLogger.isActivated()) {
                    sLogger.debug(messageType.toString()
                            + " message is marked as DELETED_REPORT_REQUESTED :" + id);
                }
                deleteStatus = DeleteStatus.DELETED_REPORT_REQUESTED;
            }
            mCmsLog.updateXmsDeleteStatus(CmsUtils.cmsFolderToContact(cmsObject.getFolder()),
                    cmsObject.getMessageId(), deleteStatus, null);
        }
    }

    private void checkNewMessages(MessageType messageType, Map<Long, CmsXmsObject> rcsMessages)
            throws FileAccessException {
        if (mNativeIds.isEmpty()) {
            return;
        }
        Set<Long> newIds = new HashSet<>(mNativeIds);
        newIds.removeAll(rcsMessages.keySet());
        long ntpTimeOffset = mSettings.getNtpLocalOffset();
        for (Long id : newIds) {
            if (MessageType.SMS == messageType) {
                SmsDataObject smsData = getSmsFromNativeProvider(id, ntpTimeOffset);
                if (smsData != null) {
                    ContactId remote = smsData.getContact();
                    if (sLogger.isActivated()) {
                        sLogger.debug("Import SMS message native Id=" + id + " contact=" + remote);
                    }
                    mXmsLog.addSms(smsData);
                    String folder = CmsUtils.contactToCmsFolder(remote);
                    CmsData.ReadStatus readStatus = CmsData.ReadStatus.READ;
                    if (Direction.INCOMING == smsData.getDirection()) {
                        readStatus = smsData.getReadStatus() == ReadStatus.UNREAD ? CmsData.ReadStatus.UNREAD
                                : CmsData.ReadStatus.READ_REPORT_REQUESTED;
                    }
                    PushStatus pushStatus = mSettings.shouldPushSms() ? PushStatus.PUSH_REQUESTED
                            : PushStatus.PUSHED;
                    mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.SMS, folder, smsData
                            .getMessageId(), pushStatus, readStatus,
                            CmsData.DeleteStatus.NOT_DELETED, smsData.getNativeId()));
                }
            } else if (MessageType.MMS == messageType) {
                for (MmsDataObject mmsData : mSmsMmsLog.getMmsFromNativeProvider(id, ntpTimeOffset)) {
                    String msgId = mmsData.getMessageId();
                    ContactId remote = mmsData.getContact();
                    if (sLogger.isActivated()) {
                        sLogger.debug("Import MMS message native Id=" + id + " contact=" + remote
                                + " mmsId=" + msgId);
                    }
                    if (Direction.OUTGOING == mmsData.getDirection()) {
                        msgId = mXmsLog.addOutgoingMms(mmsData);
                    } else {
                        mXmsLog.addIncomingMms(mmsData);
                    }
                    String folder = CmsUtils.contactToCmsFolder(remote);
                    CmsData.ReadStatus readStatus = CmsData.ReadStatus.READ;
                    if (Direction.INCOMING == mmsData.getDirection()) {
                        readStatus = mmsData.getReadStatus() == ReadStatus.UNREAD ? CmsData.ReadStatus.UNREAD
                                : CmsData.ReadStatus.READ_REPORT_REQUESTED;
                    }
                    PushStatus pushStatus = mSettings.shouldPushMms() ? PushStatus.PUSH_REQUESTED
                            : PushStatus.PUSHED;
                    mCmsLog.addXmsMessage(new CmsXmsObject(MessageType.MMS, folder, msgId, null,
                            pushStatus, readStatus, DeleteStatus.NOT_DELETED, mmsData.getNativeId()));
                }
            }
        }
    }

    private void checkReadMessages(MessageType messageType, Map<Long, CmsXmsObject> rcsMessages) {
        if (mNativeReadIds.isEmpty()) {
            return;
        }
        Set<Long> readIds = new HashSet<>(mNativeReadIds);
        readIds.retainAll(rcsMessages.keySet());
        for (Long id : readIds) {
            CmsObject cmsObject = rcsMessages.get(id);
            if (CmsData.ReadStatus.UNREAD == cmsObject.getReadStatus()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(messageType.toString()
                            + " message is marked as READ_REPORT_REQUESTED :" + id);
                }
                mCmsLog.updateXmsReadStatus(CmsUtils.cmsFolderToContact(cmsObject.getFolder()),
                        cmsObject.getMessageId(), CmsData.ReadStatus.READ_REPORT_REQUESTED, null);
            }
        }
    }

    private SmsDataObject getSmsFromNativeProvider(Long id, long ntpTimeOffset) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(SmsLog.Sms.URI, PROJECTION_SMS, SELECTION_BASE_ID,
                    new String[] {
                        String.valueOf(id)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, SmsLog.Sms.URI);
            List<SmsDataObject> smsDataObjects = mSmsMmsLog.getSmsFromNativeProvider(cursor, null,
                    ntpTimeOffset);
            if (smsDataObjects.isEmpty()) {
                return null;
            }
            return smsDataObjects.get(0);

        } finally {
            CursorUtil.close(cursor);
        }
    }

    public void execute() {
        try {
            boolean isActivated = sLogger.isActivated();
            if (isActivated) {
                sLogger.info(" >>> start sync providers");
            }
            purgeDeletedMessages();
            syncSms();
            syncMms();
            if (isActivated) {
                sLogger.info(" <<< end sync providers");
            }
        } catch (RuntimeException | FileAccessException e) {
            sLogger.error("CMS sync failure", e);

        }
    }
}
