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

import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.CmsServiceImpl;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

/**
 * Created by yplo6403 on 03/02/2016.
 */
public class XmsDeleteTask extends DeleteTask.GroupedByContactId {

    private final CmsServiceImpl mCmsService;

    /**
     * Deletion of a specific XMS.
     *
     * @param cmsService      the CMS service impl
     * @param contentResolver the content resolver
     * @param messageId       the message ID
     */
    public XmsDeleteTask(CmsServiceImpl cmsService, LocalContentResolver contentResolver, String messageId) {
        super(contentResolver, XmsData.CONTENT_URI, XmsData.KEY_MESSAGE_ID, XmsData.KEY_CONTACT,
                null, messageId);
        mCmsService = cmsService;
    }

    /**
     * Deletion of a XMS for a given contact.
     *
     * @param cmsService      the CMS service impl
     * @param contentResolver the content resolver
     * @param contact         the contact id
     */
    public XmsDeleteTask(CmsServiceImpl cmsService, LocalContentResolver contentResolver, ContactId contact) {
        super(contentResolver, XmsData.CONTENT_URI, XmsData.KEY_MESSAGE_ID, XmsData.KEY_CONTACT,
                contact);
        mCmsService = cmsService;
    }

    /**
     * Deletion of all XMS.
     *
     * @param cmsService      the CMS service impl
     * @param contentResolver the content resolver
     */
    public XmsDeleteTask(CmsServiceImpl cmsService, LocalContentResolver contentResolver) {
        super(contentResolver, XmsData.CONTENT_URI, XmsData.KEY_MESSAGE_ID, XmsData.KEY_CONTACT,
                (String) null);
        mCmsService = cmsService;
    }

    @Override
    protected void onRowDelete(ContactId groupId, String messageId) throws PayloadException {
        mCmsService.deleteMmsParts(messageId);
        mCmsService.removeXmsMessage(messageId);
    }

    @Override
    protected void onCompleted(ContactId contact, Set<String> deletedIds) {
        mCmsService.broadcastMessageDeleted(contact, deletedIds);
        mCmsService.updateDeletedFlags(contact, deletedIds);
    }
}
