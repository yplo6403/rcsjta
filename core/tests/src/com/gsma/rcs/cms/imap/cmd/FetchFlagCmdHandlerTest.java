
package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.List;

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
        
        List<FlagChange> flagChanges = handler.getResult();
        Assert.assertEquals(2, flagChanges.size());

        // Delete flagchange first
        FlagChange fg = flagChanges.get(0);
        Assert.assertEquals("myFolder",fg.getFolder());
        Assert.assertTrue(!fg.getUids().isEmpty());
        Assert.assertTrue(fg.getUids().iterator().next() == 19);
        Assert.assertTrue(fg.isDeleted());

        // Read flagchange in second
        fg = flagChanges.get(1);
        Assert.assertEquals("myFolder",fg.getFolder());
        Assert.assertTrue(!fg.getUids().isEmpty());
        Assert.assertTrue(fg.getUids().iterator().next() == 19);
        Assert.assertTrue(fg.isSeen());

    }
}
