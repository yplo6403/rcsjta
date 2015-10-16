
package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Arrays;
import java.util.List;

public class ListStatusCmdHandlerTest extends AndroidTestCase {

    public void test(){
        
        String expectedCmd = String.format(ListStatusCmdHandler.sCommand, "myFolder");    
        String[] lines  = new String[]{
                "* LIST () \".\" \"+33642575779\"",            
                "* STATUS \"+33642575779\" (UIDNEXT 4 UIDVALIDITY 1437039675 HIGHESTMODSEQ 7)",
                "* LIST () \".\" \"INBOX\"",
                "* STATUS \"INBOX\" (UIDNEXT 1 UIDVALIDITY 1437039422 HIGHESTMODSEQ 1)",
                "a3 OK List completed."
        }; 
        
        ListStatusCmdHandler handler = new ListStatusCmdHandler();       
        String cmd = handler.buildCommand("myFolder");
        Assert.assertEquals(expectedCmd, cmd);
        
        handler.handleLines(Arrays.asList(lines));
        
        Assert.assertEquals("4", handler.mData.get("+33642575779").get(Constants.METADATA_UIDNEXT));
        Assert.assertEquals("1437039675",handler.mData.get("+33642575779").get(Constants.METADATA_UIDVALIDITY));
        Assert.assertEquals("7",handler.mData.get("+33642575779").get(Constants.METADATA_HIGHESTMODSEQ));

        Assert.assertEquals("1", handler.mData.get("INBOX").get(Constants.METADATA_UIDNEXT));
        Assert.assertEquals("1437039422",handler.mData.get("INBOX").get(Constants.METADATA_UIDVALIDITY));
        Assert.assertEquals("1",handler.mData.get("INBOX").get(Constants.METADATA_HIGHESTMODSEQ));
        
        List<ImapFolder> folders = handler.getResult();
        Assert.assertEquals(2,folders.size());
        
        for(ImapFolder imapFolder : folders){
            if("+33642575779".equals(imapFolder.getName())){
                Assert.assertEquals(new Integer(4),imapFolder.getUidNext());
                Assert.assertEquals(new Integer(7),imapFolder.getHighestModseq());
                Assert.assertEquals(new Integer(1437039675),imapFolder.getUidValidity());                
            }   
            else if("INBOX".equals(imapFolder.getName())){
                Assert.assertEquals(new Integer(1),imapFolder.getUidNext());
                Assert.assertEquals(new Integer(1),imapFolder.getHighestModseq());
                Assert.assertEquals(new Integer(1437039422),imapFolder.getUidValidity());
            }
        }
        
    }
}
