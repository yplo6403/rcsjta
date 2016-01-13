package com.gsma.rcs.cms.utils;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

public class CmsUtils {

    private static final Logger sLogger = Logger.getLogger(CmsUtils.class.getSimpleName());

    public static String contactToCmsFolder(RcsSettings settings, ContactId contactId){
        return new StringBuilder(settings.getCmsDefaultDirectoryName())
                .append(settings.getCmsDirectorySeparator())
                .append(Constants.TEL_PREFIX)
                .append(contactId.toString()).toString();
    }

    public static String contactToHeader(ContactId contactId){
        return new StringBuilder(Constants.TEL_PREFIX)
                .append(contactId.toString()).toString();
    }

    public static String cmsFolderToContact(RcsSettings settings, String cmsFolder){
        String contact = StringUtils.removeQuotes(cmsFolder);
        String prefix = new StringBuilder(settings.getCmsDefaultDirectoryName())
                .append(settings.getCmsDirectorySeparator())
                .append((Constants.TEL_PREFIX)).toString();
        if(cmsFolder.startsWith(prefix)){
            contact = cmsFolder.substring(prefix.length());
        }
        return contact;
    }

    public static ContactId headerToContact(String header) {
        //TODO FGI : use regexp to extract phone number from header
        String contact = header;
        if (header.startsWith(Constants.TEL_PREFIX)) {
            contact = header.substring(Constants.TEL_PREFIX.length());
        }

        PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(contact);
        if (phoneNumber == null) {
            return null;
        }
        return ContactUtil.createContactIdFromValidatedData(phoneNumber);
    }

    public static String groupChatToCmsFolder(RcsSettings settings, String conversationId, String contributionId){
        return new StringBuilder(settings.getCmsDefaultDirectoryName())
                .append(settings.getCmsDirectorySeparator())
                .append(conversationId)
                .append(settings.getCmsDirectorySeparator())
                .append(contributionId).toString();
    }
}
