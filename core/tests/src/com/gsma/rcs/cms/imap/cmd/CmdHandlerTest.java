package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.cmd.CmdHandler.CommandType;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

public class CmdHandlerTest extends AndroidTestCase {

    public void test(){
        List<String> capabilities = new ArrayList<String>();
        capabilities.add(Constants.CAPA_CONDSTORE);
        CmdHandler handler;
        
        handler = CmdHandler.getHandler(CommandType.SELECT_CONDSTORE, capabilities);
        assertTrue(handler instanceof SelectCondstoreCmdHandler);

        handler = CmdHandler.getHandler(CommandType.LIST_STATUS, capabilities);
        assertTrue(handler instanceof ListStatusCmdHandler);

        handler = CmdHandler.getHandler(CommandType.FETCH_FLAGS, capabilities, "myFolder");
        assertTrue(handler instanceof FetchFlagCmdHandler);
        
        handler = CmdHandler.getHandler(CommandType.FETCH_HEADERS, capabilities);
        assertTrue(handler instanceof FetchHeaderCmdHandler);

        handler = CmdHandler.getHandler(CommandType.FETCH_MESSAGES_BODY, capabilities);
        assertTrue(handler instanceof FetchMessageCmdHandler);
               
    }
    
}
