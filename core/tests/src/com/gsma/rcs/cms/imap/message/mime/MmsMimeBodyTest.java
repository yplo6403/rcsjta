package com.gsma.rcs.cms.imap.message.mime;

import android.test.AndroidTestCase;

import com.gsma.rcs.cms.Constants;

public class MmsMimeBodyTest extends AndroidTestCase {

    public static StringBuilder bodyContent = new StringBuilder();
    public static StringBuilder bodyContentSmil = new StringBuilder();

    public static StringBuilder smilContent = new StringBuilder()
    .append("<smil>").append(Constants.CRLF)
    .append("<head>").append(Constants.CRLF)
    .append("    <layout >").append(Constants.CRLF)
    .append("        <root - layout height='720' width='480' / >").append(Constants.CRLF)
    .append("        <region fit = 'meet' height='100 % ' left='0' top='0' width='100 % ' id='Text'/>").append(Constants.CRLF)
    .append("        <region fit='meet' height='100 % ' left='0' top='0' width='100 % ' id='Image'/>").append(Constants.CRLF)
    .append("    </layout>").append(Constants.CRLF)
    .append("</head>").append(Constants.CRLF)
    .append("<body>").append(Constants.CRLF)
    .append("    <par dur='5000ms'>").append(Constants.CRLF)
    .append("        <img src='002_3.jpg' region='Image'/>").append(Constants.CRLF)
    .append("    </par>").append(Constants.CRLF)
    .append("    <par dur='5000ms'>").append(Constants.CRLF)
    .append("        <text src='text_0.txt' region='Text'/>").append(Constants.CRLF)
    .append("    </par>").append(Constants.CRLF)
    .append("</body>").append(Constants.CRLF)
    .append("</smil>");

    static {
        bodyContent.
                append("Content-Type: Multipart/Related;boundary=\"boundary_1446218793256\";").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("--boundary_1446218793256").append(Constants.CRLF)
                .append("Content-Type: text/plain").append(Constants.CRLF)
                .append("Content-ID: <text_0.txt>").append(Constants.CRLF)
                .append("Content-Transfer-Encoding: base64").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("myContent").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("--boundary_1446218793256--");

        bodyContentSmil.
                append("Content-Type: Multipart/Related;boundary=\"boundary_1446218793256\";").append("start=<smil>;type=application/smil").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("--boundary_1446218793256").append(Constants.CRLF)
                .append("Content-Type: application/smil").append(Constants.CRLF)
                .append("Content-ID: <smil>").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append(smilContent.toString()).append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("--boundary_1446218793256").append(Constants.CRLF)
                .append("Content-Type: text/plain").append(Constants.CRLF)
                .append("Content-ID: <text_0.txt>").append(Constants.CRLF)
                .append("Content-Transfer-Encoding: base64").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("myContent").append(Constants.CRLF)
                .append(Constants.CRLF)
                .append("--boundary_1446218793256--");
    }

    public void test(){

        MmsMimeBody mimebody = new MmsMimeBody();
        mimebody.parsePayload(bodyContent.toString());

        MultiPart multipart = mimebody.mMultiParts.get(0);
        assertEquals("text/plain", multipart.getContentType());
        assertEquals("<text_0.txt>", multipart.getContentId());
        assertEquals("base64", multipart.getContentTransferEncoding());
        assertEquals("myContent", multipart.getContent());

        mimebody = new MmsMimeBody();
        mimebody.setBoundary("boundary_1446218793256");
        mimebody.addMultiPart("text/plain", "<text_0.txt>", "base64", "myContent");
        assertEquals(bodyContent.toString(), mimebody.toString());
    }

    public void testWithSmil(){

        MmsMimeBody mimebody = new MmsMimeBody();
        mimebody.parsePayload(bodyContentSmil.toString());

        MultiPart multipart = mimebody.mMultiParts.get(0);
        assertEquals("application/smil", multipart.getContentType());
        assertEquals("<smil>", multipart.getContentId());
        assertEquals(null, multipart.getContentTransferEncoding());
        assertEquals(smilContent.toString(), multipart.getContent());

        multipart = mimebody.mMultiParts.get(1);
        assertEquals("text/plain", multipart.getContentType());
        assertEquals("<text_0.txt>", multipart.getContentId());
        assertEquals("base64", multipart.getContentTransferEncoding());
        assertEquals("myContent", multipart.getContent());

        mimebody = new MmsMimeBody();
        mimebody.setBoundary("boundary_1446218793256");
        mimebody.addMultiPart("application/smil", "<smil>", null, smilContent.toString());
        mimebody.addMultiPart("text/plain", "<text_0.txt>", "base64", "myContent");
        assertEquals(bodyContentSmil.toString(), mimebody.toString());
    }

}
