
package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Arrays;

public class SelectCondstoreCmdHandlerTest extends AndroidTestCase {

    public void test(){
        
        String expectedCmd = String.format(SelectCondstoreCmdHandler.sCommand, "myFolder");    
        String[] lines  = new String[]{
        "* OK [CLOSED] Previous mailbox closed.",
        "* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)",
        "* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft \\*)] Flags permitted.",
        "* 3 EXISTS",
        "* 0 RECENT",
        "* OK [UIDVALIDITY 1437039675] UIDs valid",
        "* OK [UIDNEXT 4] Predicted next UID",
        "* OK [HIGHESTMODSEQ 7] Highest",
        "a6 OK [READ-WRITE] Select completed."
        }; 
        
        
        String[] capabilities = new String[]{Constants.CAPA_CONDSTORE};
        
        SelectCondstoreCmdHandler handler = new SelectCondstoreCmdHandler();       
        Assert.assertTrue(handler.checkCapabilities(Arrays.asList(capabilities)));
        
        String cmd = handler.buildCommand("myFolder");
        Assert.assertEquals(expectedCmd, cmd);
        
        handler.handleLines(Arrays.asList(lines));
        
        Assert.assertEquals("1437039675",handler.mData.get("myFolder").get(Constants.METADATA_UIDVALIDITY));
        Assert.assertEquals("4", handler.mData.get("myFolder").get(Constants.METADATA_UIDNEXT));        
        Assert.assertEquals("7",handler.mData.get("myFolder").get(Constants.METADATA_HIGHESTMODSEQ));
    }
    

}
