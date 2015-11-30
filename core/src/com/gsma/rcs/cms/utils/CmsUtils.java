package com.gsma.rcs.cms.utils;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.provider.settings.RcsSettings;
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
        String contact = cmsFolder;
        String prefix = new StringBuilder(settings.getCmsDefaultDirectoryName())
                .append(settings.getCmsDirectorySeparator())
                .append((Constants.TEL_PREFIX)).toString();
        if(cmsFolder.startsWith(prefix)){
            contact = cmsFolder.substring(prefix.length());
        }
        return contact;
    }

    public static String headerToContact(String header){
        String contact = header;
        String prefix = new StringBuilder(Constants.TEL_PREFIX).toString();
        if(header.startsWith(prefix)){
            contact = header.substring(prefix.length());
        }
        return contact;
    }
}
