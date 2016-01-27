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

package com.gsma.rcs.cms.imap.message.cpim.multipart;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.HeaderPart;
import com.gsma.rcs.cms.imap.message.cpim.CpimBody;
import com.gsma.rcs.utils.logger.Logger;

import com.sonymobile.rcs.imap.Header;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultipartCpimBody extends CpimBody {

    private static final Logger sLogger = Logger.getLogger(MultipartCpimBody.class.getSimpleName());

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
                mHeaders.addHeader(header.getKey(), header.getValue());
            }

            if (!checkMultipart(getContentType())) {
                StringBuilder message = new StringBuilder("This content type is not supported")
                        .append(Constants.CRLF).append(body);
                new RuntimeException(message.toString());
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
            mBoundary = new StringBuilder(C_BOUNDARY).append(System.currentTimeMillis()).toString();
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
        if (contentType.startsWith(Constants.CONTENT_TYPE_MULTIPART_RELATED)) {
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
        String delimiter = new StringBuilder(Constants.CRLF).append(Constants.BOUDARY_SEP)
                .append(mBoundary).toString();
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
            StringBuilder sb = new StringBuilder();
            sb.append(mHeaderPart);
            sb.append(Constants.CRLF);
            sb.append(mContent);
            sb.append(Constants.CRLF);
            return sb.toString();
        }

        private void fromPayload(String partContent) {
            String[] parts = partContent.split(Constants.CRLFCRLF, 2);
            parseHeaders(parts[0]);
            mContent = parts[1].substring(0, parts[1].lastIndexOf(Constants.CRLF));
        }

        private void parseHeaders(String headerContent) {
            for (String header : headerContent.split(Constants.CRLF)) {
                String[] val = header.split(Constants.HEADER_SEP);
                mHeaderPart.addHeader(val[0].trim(), val[1].trim());
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
