
package com.gsma.rcs.cms;

public class Constants {

    public static final String CMD_LIST = "LIST";
    public static final String CMD_STATUS = "STATUS";
    public static final String CMD_LIST_STATUS = "LIST \"\" * RETURN (STATUS (UIDNEXT UIDVALIDITY HIGHESTMODSEQ))";
    public static final String CMD_SELECT = "SELECT \"%1$s\"";
    public static final String CMD_SELECT_CONDSTORE = "SELECT \"%1$s\" (CONDSTORE)";
    public static final String CMD_FETCH_FLAGS = "UID FETCH 1:%1$s (UID FLAGS) (CHANGEDSINCE %2$s)";
    // public static final String CMD_FETCH_HEADERS = "UID FTECH %1$s:%2$s (RFC822.SIZE FLAGS MOSDEQ
    // BODY.PEEK[HEADER.FIELDS(FROM TO)])";
    public static final String CMD_FETCH_HEADERS = "UID FETCH %1$s:%2$s (FLAGS MODSEQ BODY.PEEK[HEADER])";
    public static final String CMD_FETCH_MESSAGE = "UID FETCH %1$s (RFC822.SIZE FLAGS MODSEQ BODY.PEEK[])";

    public static final String CAPA_CONDSTORE = "CONDSTORE";
    public static final String CAPA_QRESYNC = "QRESYNC";

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
    public static final String HEADER_IMDN_MESSAGE_ID = "IMDN-Message-ID";
    public static final String HEADER_DIRECTION = "Message-Direction";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_ID = "Content-ID";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String HEADER_BASE64 = "base64";

    public static final String BOUNDARY = "boundary=";
    public static final String BOUDARY_SEP = "--";

    public static final String CONTENT_TYPE_CPIM = "Message/CPIM";
    public static final String PAGER_MESSAGE = "\"pager-message\"";
    public static final String MULTIMEDIA_MESSAGE = "\"multimedia-message\"";
    public static final String DIRECTION_SENT = "sent";
    public static final String DIRECTION_RECEIVED = "received";

    public static final String TEL_PREFIX = "tel:";

    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CONTENT_TYPE_MULTIPART_RELATED = "Multipart/Related";
    public static final String CONTENT_TYPE_APP_SMIL = "application/smil";
    public static final String CONTENT_TYPE_IMAGE_JPG = "image/jpg";
    public static final String CONTENT_TYPE_IMAGE_JPEG = "image/jpeg";
    public static final String CONTENT_TYPE_IMAGE_PNG = "image/png";
    public static final String CONTENT_TYPE_IMAGE_GIF = "image/gif";
    public static final String CONTENT_TYPE_IMAGE_BMP = "image/bmp";

}
