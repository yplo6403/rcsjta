/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.xms;

import com.gsma.rcs.core.cms.service.CmsSessionController;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.cms.CmsLog;
import com.gsma.rcs.provider.cms.CmsObject;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

/**
 * A class to delete XMS
 *
 * @author Philippe LEMORDANT
 */
public class XmsDeleteTask extends DeleteTask.GroupedByContactId {

    private final CmsServiceImpl mCmsService;
    private final CmsSessionController mCmsSessionCrtl;
    private final CmsLog mCmsLog;

    /**
     * Constructor to delete of a specific XMS after having received a delete event from CMS.
     *
     * @param cmsService the CMS service impl
     * @param contentResolver the content resolver
     * @param messageId the message ID
     * @param contact the contact ID
     * @param cmsLog the CMS log accessor
     */
    public XmsDeleteTask(CmsServiceImpl cmsService, LocalContentResolver contentResolver,
            ContactId contact, String messageId, CmsLog cmsLog) {
        super(contentResolver, XmsData.CONTENT_URI, XmsData.KEY_MESSAGE_ID, false,
                XmsData.KEY_CONTACT, true, XmsData.KEY_MESSAGE_ID + "=? AND " + XmsData.KEY_CONTACT
                        + "='" + contact.toString() + "'", messageId);
        mCmsService = cmsService;
        mCmsSessionCrtl = null;
        mCmsLog = cmsLog;
    }

    /**
     * Constructor to delete of a specific XMS after having received a request from local client.
     *
     * @param cmsService the CMS service impl
     * @param contentResolver the content resolver
     * @param contact the contact ID
     * @param messageId the message ID
     * @param cmsSessionCtrl the CMS session controller
     */
    public XmsDeleteTask(CmsServiceImpl cmsService, LocalContentResolver contentResolver,
            ContactId contact, String messageId, CmsSessionController cmsSessionCtrl) {
        super(contentResolver, XmsData.CONTENT_URI, XmsData.KEY_MESSAGE_ID, false,
                XmsData.KEY_CONTACT, true, XmsData.KEY_MESSAGE_ID + "=? AND " + XmsData.KEY_CONTACT
                        + "='" + contact.toString() + "'", messageId);
        mCmsService = cmsService;
        mCmsSessionCrtl = cmsSessionCtrl;
        mCmsLog = null;
    }

    /**
     * Deletion of all XMS for a given contact.
     *
     * @param cmsService the CMS service impl
     * @param contentResolver the content resolver
     * @param contact the contact id
     * @param cmsSessionCtrl the CMS session controller
     */
    public XmsDeleteTask(CmsServiceImpl cmsService, LocalContentResolver contentResolver,
            ContactId contact, CmsSessionController cmsSessionCtrl) {
        super(contentResolver, XmsData.CONTENT_URI, XmsData.KEY_MESSAGE_ID, false,
                XmsData.KEY_CONTACT, contact);
        mCmsService = cmsService;
        mCmsSessionCrtl = cmsSessionCtrl;
        mCmsLog = null;
    }

    /**
     * Deletion of all XMS.
     *
     * @param cmsService the CMS service impl
     * @param contentResolver the content resolver
     * @param cmsSessionCtrl the CMS session controller
     */
    public XmsDeleteTask(CmsServiceImpl cmsService, LocalContentResolver contentResolver,
            CmsSessionController cmsSessionCtrl) {
        super(contentResolver, XmsData.CONTENT_URI, XmsData.KEY_MESSAGE_ID, false,
                XmsData.KEY_CONTACT, false, null);
        mCmsService = cmsService;
        mCmsSessionCrtl = cmsSessionCtrl;
        mCmsLog = null;
    }

    @Override
    protected void onRowDelete(ContactId contactId, String messageId) throws PayloadException {
        mCmsService.removeXmsMessage(contactId, messageId);
        mCmsService.deleteMmsParts(contactId, messageId);
    }

    @Override
    protected void onCompleted(ContactId contact, Set<String> deletedIds) {
        mCmsService.broadcastMessageDeleted(contact, deletedIds);
        if (mCmsSessionCrtl != null) {
            mCmsSessionCrtl.onDeleteXmsMessages(contact, deletedIds);
        } else {
            for (String delId : deletedIds) {
                mCmsLog.updateXmsDeleteStatus(contact, delId, CmsObject.DeleteStatus.DELETED, null);
            }
        }
    }
}
