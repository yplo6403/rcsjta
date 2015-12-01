package com.gsma.rcs.cms.utils;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.provider.settings.CmsSettings;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;

public class CmsUtils {

    private static final Logger sLogger = Logger.getLogger(CmsUtils.class.getSimpleName());

    public static String contactToCmsFolder(CmsSettings settings, String contact){
        return new StringBuilder(settings.getCmsDefaultDirectory())
                .append(settings.getCmsDirectorySeparator())
                .append(Constants.TEL_PREFIX)
                .append(contact).toString();
    }

    public static String contactToHeader(String contact){
        return new StringBuilder(Constants.TEL_PREFIX)
                .append(contact).toString();
    }

    public static String cmsFolderToContact(CmsSettings settings, String cmsFolder){
        String contact = StringUtils.removeQuotes(cmsFolder);
        String prefix = new StringBuilder(settings.getCmsDefaultDirectory())
                .append(settings.getCmsDirectorySeparator())
                .append((Constants.TEL_PREFIX)).toString();
        if(cmsFolder.startsWith(prefix)){
            contact = cmsFolder.substring(prefix.length());
        }
        return contact;
    }

    public static String headerToContact(String header){
        String contact = header;
        String prefix = Constants.TEL_PREFIX;
        if(header.startsWith(prefix)){
            contact = header.substring(prefix.length());
        }
        return contact;
    }
}
