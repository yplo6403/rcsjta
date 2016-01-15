package com.gsma.rcs.cms.imap.message.groupstate;

import android.content.Context;
import android.test.AndroidTestCase;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.services.rcs.contact.ContactUtil;

import junit.framework.Assert;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by VGZL8743 on 14/01/2016.
 */
public class GroupStateParserTest extends AndroidTestCase {

    private final String CRLF = "\r\n";


    public void test(){

        try {
            ContactUtil.getInstance(getContext());
            StringBuffer xml = new StringBuffer()
                    .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(CRLF)
                    .append("<groupstate").append(CRLF)
                    .append("timestamp=\"2012-06-13T16:39:57-05:00\"" ).append(CRLF)
                    .append("lastfocussessionid=\"sip:da9274453@company.com\"").append(CRLF)
                    .append("group-type=\"Closed\">").append(CRLF)
                    .append("<participant name=\"bob\" comm-addr=\"tel:+16135551210\"/>").append(CRLF)
                    .append("<participant name=\"alice\" comm-addr=\"tel:+16135551211\"/>").append(CRLF)
                    .append("<participant name=\"donald\" comm-addr=\"tel:+16135551212\"/>").append(CRLF)
                    .append("</groupstate>").append(CRLF);

            InputSource source = new InputSource(new ByteArrayInputStream(xml.toString().getBytes()));
            GroupStateParser parser = new GroupStateParser(source);
            parser.parse();
            GroupStateDocument doc = parser.getGroupStateDocument();
            Assert.assertEquals("2012-06-13T16:39:57-05:00", doc.getTimestamp());
            Assert.assertEquals("sip:da9274453@company.com", doc.getLastfocussessionid());
            Assert.assertEquals(3, doc.getParticipants().size());
            Assert.assertEquals("+16135551210", doc.getParticipants().get(0).toString());
            Assert.assertEquals("+16135551211", doc.getParticipants().get(1).toString());
            Assert.assertEquals("+16135551212", doc.getParticipants().get(2).toString());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }
}
