
package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Set;

public class FetchFlagCmdHandlerTest extends AndroidTestCase {

    public void test(){
        
        String expectedCmd = String.format(FetchFlagCmdHandler.sCommand, "1","2");    
        String line = "(UID 19 FLAGS (\\Seen \\Deleted) MODSEQ (92566))";
        
        FetchFlagCmdHandler handler = new FetchFlagCmdHandler("myFolder");        
        String cmd = handler.buildCommand("1","2");
        Assert.assertEquals(expectedCmd, cmd);
        
        handler.handleLine(line);
        
        Assert.assertEquals("19", handler.mData.get(19).get(Constants.METADATA_UID));
        Assert.assertEquals("\\Seen \\Deleted",handler.mData.get(19).get(Constants.METADATA_FLAGS));
        Assert.assertEquals("92566",handler.mData.get(19).get(Constants.METADATA_MODSEQ));
        
        Set<FlagChange> flagChanges = handler.getResult();
        Assert.assertEquals(1,flagChanges.size());
        FlagChange fg = flagChanges.iterator().next();
        Assert.assertEquals("myFolder",fg.getFolder());
        Assert.assertTrue(19==fg.getUid());
        Assert.assertTrue(fg.addSeenFlag());
        Assert.assertTrue(fg.addDeletedFlag());
    }
}
