/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.cms.protocol.message.cpim.multipart;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.protocol.message.HeaderPart;
import com.gsma.rcs.core.cms.protocol.message.cpim.CpimBody;
import com.gsma.rcs.imaplib.imap.Header;
import com.gsma.rcs.platform.ntp.NtpTrustedTime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultipartCpimBody extends CpimBody {

    private static final String C_BOUNDARY = "boundary_";

    private final List<Part> mParts;

    private String mBoundary;

    public MultipartCpimBody() {
        super();
        mParts = new ArrayList<>();
    }

    @Override
    protected void parseBody(String body) {
        String[] parts = body.split(Constants.CRLFCRLF, 2);
        if (2 == parts.length) {
            for (Header header : Header.parseHeaders(parts[0]).values()) {
                mHeaders.addHeader(header.getKey().toLowerCase(), header.getValue());
            }
            if (!checkMultipart(getContentType())) {
                throw new RuntimeException("This content type is not supported" + Constants.CRLF + body);
            }
            // remove first boundary and last boundary separator
            int length = Constants.BOUDARY_SEP.length() + mBoundary.length();
            String multipartContent = parts[1].substring(length,
                    parts[1].lastIndexOf(Constants.BOUDARY_SEP));
            checkParts(multipartContent);
        }
    }

    @Override
    protected String toPayload() {
        StringBuilder sb = new StringBuilder();
        if (mBoundary == null) {
            mBoundary = C_BOUNDARY + NtpTrustedTime.currentTimeMillis();
        }
        // content type : multipart/related
        sb.append(Constants.HEADER_CONTENT_TYPE).append(Constants.HEADER_SEP)
                .append(Constants.CONTENT_TYPE_MULTIPART_RELATED).append(";")
                .append(Constants.BOUNDARY).append("\"").append(mBoundary).append("\";");
        sb.append(Constants.CRLFCRLF);
        for (Part part : mParts) {
            sb.append(Constants.BOUDARY_SEP).append(mBoundary).append(Constants.CRLF);
            sb.append(part);
            sb.append(Constants.CRLF);
        }
        sb.append(Constants.BOUDARY_SEP).append(mBoundary).append(Constants.BOUDARY_SEP);
        return sb.toString();
    }

    public void addMultiPart(HeaderPart headerPart, String content) {
        mParts.add(new Part(headerPart, content));
    }

    private boolean checkMultipart(String contentType) {
        boolean isMultipart = false;
        if (contentType.toLowerCase().contains(Constants.CONTENT_TYPE_MULTIPART_RELATED)) {
            isMultipart = true;
        }
        String pattern = "^.*boundary=\"(.*)\".*$";
        Matcher matcher = Pattern.compile(pattern).matcher(contentType);
        if (matcher.matches()) {
            mBoundary = matcher.group(1);
        }
        return isMultipart && mBoundary != null;
    }

    private void checkParts(String content) {
        String delimiter = Constants.CRLF + Constants.BOUDARY_SEP + mBoundary;
        for (String mimePart : content.split(delimiter)) {
            if (mimePart.startsWith(Constants.CRLF)) {
                mimePart = mimePart.substring(Constants.CRLF.length());
            }
            if (mimePart.isEmpty()) {
                continue;
            }
            addPart(new Part(mimePart));
        }
    }

    private void addPart(Part part) {
        mParts.add(part);
    }

    public void setBoundary(String boundary) {
        mBoundary = boundary;
    }

    public List<Part> getParts() {
        return mParts;
    }

    public static class Part {

        private final HeaderPart mHeaderPart;
        private String mContent;

        public Part(String payload) {
            mHeaderPart = new HeaderPart();
            fromPayload(payload);
        }

        public Part(HeaderPart headerPart, String content) {
            mHeaderPart = headerPart;
            mContent = content;
        }

        public String toString() {
            return String.valueOf(mHeaderPart) + Constants.CRLF + mContent + Constants.CRLF;
        }

        private void fromPayload(String partContent) {
            String[] parts = partContent.split(Constants.CRLFCRLF, 2);
            if (2 == parts.length) {
                parseHeaders(parts[0]);
                mContent = parts[1].substring(0, parts[1].lastIndexOf(Constants.CRLF));
            }
        }

        private void parseHeaders(String headerContent) {
            for (Header header : Header.parseHeaders(headerContent).values()) {
                mHeaderPart.addHeader(header.getKey().toLowerCase(), header.getValue());
            }
        }

        public String getHeader(String headerName) {
            return mHeaderPart.getHeaderValue(headerName);
        }

        public String getContentType() {
            return getHeader(Constants.HEADER_CONTENT_TYPE);
        }

        public String getContentTransferEncoding() {
            return getHeader(Constants.HEADER_CONTENT_TRANSFER_ENCODING);
        }

        public String getContentId() {
            return getHeader(Constants.HEADER_CONTENT_ID);
        }

        public String getContent() {
            return mContent;
        }
    }
}
