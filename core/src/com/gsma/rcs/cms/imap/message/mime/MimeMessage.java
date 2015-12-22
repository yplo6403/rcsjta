/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms.imap.message.mime;

import com.gsma.rcs.cms.Constants;
import java.util.List;

public abstract class MimeMessage {

    List<MimeHeaders> mMimeHeaders;
    MimeBody mMimeBody;

    public void addHeaderPart(MimeHeaders mimeHeaders){
        mMimeHeaders.add(mimeHeaders);
    }

    public void setBodyPart(MimeBody mimeBody){
        mMimeBody = mimeBody;
    }

    public String getBodyPart(){
        return (mMimeBody ==null ? null : mMimeBody.toString());
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(MimeHeaders headerPart : mMimeHeaders){
            sb.append(headerPart).append(Constants.CRLF);
        }
        sb.append(mMimeBody);
        return sb.toString();
    }
}
