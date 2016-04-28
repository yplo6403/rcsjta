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

package com.gsma.rcs.core.cms;

public class Constants {

    public static final String CMS_ROOT_DIRECTORY = "Default";
    public static final String CMS_DIRECTORY_SEPARATOR = "/";
    public static final String CMD_LIST_STATUS = "LIST \"\" \"" + CMS_ROOT_DIRECTORY
            + CMS_DIRECTORY_SEPARATOR
            + "*\" RETURN (STATUS (MESSAGES UIDNEXT UIDVALIDITY HIGHESTMODSEQ))";
    public static final String CMD_LIST = "LIST \"\" \"" + CMS_ROOT_DIRECTORY
            + CMS_DIRECTORY_SEPARATOR + "*\"";
    public static final String CMD_SELECT_CONDSTORE = "SELECT %1$s (CONDSTORE)";
    public static final String CMD_FETCH_FLAGS = "UID FETCH 1:%1$s (UID FLAGS) (CHANGEDSINCE %2$s)";
    public static final String CMD_UID_SEARCH = "UID SEARCH %1$s";
    // public static final String CMD_FETCH_HEADERS = "UID FTECH %1$s:%2$s (RFC822.SIZE FLAGS MOSDEQ
    // BODY.PEEK[HEADER.FIELDS(FROM TO)])";
    public static final String CMD_FETCH_HEADERS = "UID FETCH %1$s:%2$s (FLAGS MODSEQ BODY.PEEK[HEADER])";
    public static final String CMD_FETCH_MESSAGE = "UID FETCH %1$s (RFC822.SIZE FLAGS MODSEQ BODY.PEEK[])";

    public static final String CAPA_CONDSTORE = "CONDSTORE";

    public static final String METADATA_MESSAGES = "MESSAGES";
    public static final String METADATA_FLAGS = "FLAGS";
    public static final String METADATA_UID = "UID";
    public static final String METADATA_UIDVALIDITY = "UIDVALIDITY";
    public static final String METADATA_HIGHESTMODSEQ = "HIGHESTMODSEQ";
    public static final String METADATA_MODSEQ = "MODSEQ";
    public static final String METADATA_UIDNEXT = "UIDNEXT";
    public static final String METADATA_SIZE = "RFC822.SIZE";

    public static final String OK = "OK";

    public static final String CRLF = "\r\n";
    public static final String CRLFCRLF = "\r\n\r\n";

    public static final String HEADER_SEP = ": ";
    public static final String HEADER_FROM = "From";
    public static final String HEADER_TO = "To";
    public static final String HEADER_SUBJECT = "Subject";
    public static final String HEADER_MESSAGE_CONTEXT = "Message-Context";
    public static final String HEADER_MESSAGE_CORRELATOR = "Message-Correlator";
    public static final String HEADER_MESSAGE_ID = "Message-ID";
    public static final String HEADER_CONVERSATION_ID = "Conversation-ID";
    public static final String HEADER_CONTRIBUTION_ID = "Contribution-ID";
    public static final String HEADER_DATE = "Date";
    public static final String HEADER_DATE_TIME = "DateTime";
    public static final String HEADER_IMDN_MESSAGE_ID = "IMDN-Message-ID";
    public static final String HEADER_DIRECTION = "Message-Direction";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_ID = "Content-ID";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String HEADER_BASE64 = "base64";

    public static final String BOUNDARY = "boundary=";
    public static final String BOUDARY_SEP = "--";

    /*
     * Below strings are inserted into Content-Type header. Therefore they are sent to CMS in lower
     * case but comparison with Content-type received from CMS shall be done case un-sensitive.
     */
    public static final String MESSAGE_CPIM = "message/cpim";
    public static final String APPLICATION_CPM_SESSION = "application/x-cpm-session";
    public static final String APPLICATION_GROUP_STATE = "application/group-state-object+xml";
    public static final String APPLICATION_FILE_TRANSFER = "application/x-cpm-file-transfer";
    public static final String PAGER_MESSAGE = "pager-message";
    public static final String MULTIMEDIA_MESSAGE = "multimedia-message";
    public static final String DIRECTION_SENT = "sent";
    public static final String DIRECTION_RECEIVED = "received";

    public static final String TEL_PREFIX = "tel:";

    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_MESSAGE_IMDN_XML = "message/imdn+xml";
    public static final String CONTENT_TYPE_MULTIPART_RELATED = "multipart/related";

}
