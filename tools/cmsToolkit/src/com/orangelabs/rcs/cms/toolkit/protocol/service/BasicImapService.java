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

package com.orangelabs.rcs.cms.toolkit.protocol.service;

import com.orangelabs.rcs.cms.toolkit.protocol.cmd.CmdHandler;
import com.orangelabs.rcs.cms.toolkit.protocol.cmd.CmdHandler.CommandType;
import com.orangelabs.rcs.cms.toolkit.protocol.cmd.ImapFolder;
import com.orangelabs.rcs.cms.toolkit.protocol.cmd.ListCmdHandler;
import com.orangelabs.rcs.cms.toolkit.protocol.cmd.ListStatusCmdHandler;

import com.gsma.rcs.imaplib.imap.DefaultImapService;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapUtil;
import com.gsma.rcs.imaplib.imap.IoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BasicImapService extends DefaultImapService {

    /**
     * @param ioService
     */
    public BasicImapService(IoService ioService) {
        super(ioService);
    }

    /**
     * Execute LIST command on CMS server
     * 
     * @return
     * @throws IOException
     * @throws ImapException
     */
    public List<String> list() throws IOException, ImapException {
        ListCmdHandler handler = (ListCmdHandler) CmdHandler.getHandler(CommandType.LIST,
                getCapabilities());
        writeCommand(handler.buildCommand());
        handler.handleLines(readToEndOfResponse());
        return handler.getResult();
    }

    /**
     * Execute LIST-STATUS command on CMS server
     * 
     * @return
     * @throws IOException
     * @throws ImapException
     */
    public List<ImapFolder> listStatus() throws IOException, ImapException {
        ListStatusCmdHandler handler = (ListStatusCmdHandler) CmdHandler.getHandler(
                CommandType.LIST_STATUS, getCapabilities());
        writeCommand(handler.buildCommand());
        handler.handleLines(readToEndOfResponse());
        return handler.getResult();
    }

    /**
     * Execute SELECT CONDSTORE command on CMS server
     * 
     * @param folderName
     * @throws IOException
     * @throws ImapException
     */
    public synchronized void selectCondstore(String folderName) throws IOException, ImapException {

        CmdHandler handler = CmdHandler.getHandler(CommandType.SELECT_CONDSTORE, getCapabilities());
        writeCommand(handler.buildCommand(folderName));
        readToEndOfResponse();
    }

    /**
     * Execute FETCH MESSAGE command on CMS server (one message)
     * 
     * @param uid
     * @return ImapMessage
     * @throws IOException
     * @throws ImapException
     */
    public ImapMessage fetchMessage(Integer uid) throws IOException, ImapException {

        CmdHandler handler = CmdHandler.getHandler(CommandType.FETCH_MESSAGES_BODY,
                getCapabilities());
        synchronized (getIoService()) {
            writeCommand(handler.buildCommand(uid));
            String line;
            while (true) {
                line = ioReadLine();
                checkResponseNotBad(line);
                if (isTagged(line)) {
                    break;
                }
                if (handler.handleLine(line)) {
                    handler.handlePart(readPart(line));
                }
            }
            checkResponseOk(line);
        }
        return (ImapMessage) handler.getResult();
    }

    /**
     * Execute FETCH MESSAGES command on CMS server (fetch all messages)
     * 
     * @return
     * @throws IOException
     * @throws ImapException
     */
    public List<ImapMessage> fetchAllMessages() throws IOException, ImapException {

        List<ImapMessage> messages = new ArrayList<ImapMessage>();
        CmdHandler handler = CmdHandler.getHandler(CommandType.FETCH_MESSAGES_BODY,
                getCapabilities());
        synchronized (getIoService()) {
            writeCommand(handler.buildCommand("1:*"));
            String line;
            while (true) {
                line = ioReadLine();
                checkResponseNotBad(line);
                if (isTagged(line)) {
                    break;
                }
                if (handler.handleLine(line)) {
                    handler.handlePart(readPart(line));
                    messages.add((ImapMessage) handler.getResult());
                }
            }
            checkResponseOk(line);
        }
        return messages;
    }

    public synchronized int append(String folderName, List<Flag> flags, String payload)
            throws IOException, ImapException {
        // append INBOX (\Seen) {310}
        int length = payload.getBytes().length;

        if (!folderName.startsWith("\"")) {
            folderName = new StringBuilder("\"").append(folderName).append("\"").toString();
        }
        writeCommand("APPEND", folderName, ImapUtil.getFlagsAsString(flags), "{" + length + "}");
        String ok = ioReadLine();
        if (!ok.startsWith("+"))
            return -1;
        ioWriteln(payload);

        while (true) {
            ok = ioReadLine();
            if (isTagged(ok)) {
                break;
            }
        }

        checkResponseNotBad(ok);

        return getUidPlus(ok);
    }
}
