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

package com.gsma.rcs.core.cms.protocol.service;

import com.gsma.rcs.core.cms.protocol.cmd.CmdHandler;
import com.gsma.rcs.core.cms.protocol.cmd.CmdHandler.CommandType;
import com.gsma.rcs.core.cms.protocol.cmd.FetchFlagCmdHandler;
import com.gsma.rcs.core.cms.protocol.cmd.FetchHeaderCmdHandler;
import com.gsma.rcs.core.cms.protocol.cmd.ImapFolder;
import com.gsma.rcs.core.cms.protocol.cmd.ListCmdHandler;
import com.gsma.rcs.core.cms.protocol.cmd.ListStatusCmdHandler;
import com.gsma.rcs.core.cms.protocol.cmd.UidSearchCmdHandler;
import com.gsma.rcs.core.cms.sync.process.FlagChangeOperation;
import com.gsma.rcs.imaplib.imap.DefaultImapService;
import com.gsma.rcs.imaplib.imap.Flag;
import com.gsma.rcs.imaplib.imap.ImapException;
import com.gsma.rcs.imaplib.imap.ImapMessage;
import com.gsma.rcs.imaplib.imap.ImapUtil;
import com.gsma.rcs.imaplib.imap.IoService;

import java.io.IOException;
import java.util.List;

public class BasicImapService extends DefaultImapService {

    /**
     * @param ioService the IO sezrvice
     */
    public BasicImapService(IoService ioService) {
        super(ioService);
    }

    /**
     * Execute LIST command on CMS server
     * 
     * @return the list of string folders
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
     * @return the list of IMAP folders
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
     * @param folderName the folder name
     * @throws IOException
     * @throws ImapException
     */
    public synchronized void selectCondstore(String folderName) throws IOException, ImapException {

        CmdHandler handler = CmdHandler.getHandler(CommandType.SELECT_CONDSTORE, getCapabilities());
        writeCommand(handler.buildCommand(folderName));
        readToEndOfResponse();
    }

    /**
     * Execute FETCH FLAGS command on CMS server
     * 
     * @param folderName the folder name
     * @param uid the UID
     * @param changedSince the changed since
     * @return the list of FlagChangeOperation
     * @throws IOException
     * @throws ImapException
     */
    public List<FlagChangeOperation> fetchFlags(String folderName, Integer uid, Integer changedSince)
            throws IOException, ImapException {

        FetchFlagCmdHandler handler = (FetchFlagCmdHandler) CmdHandler.getHandler(
                CommandType.FETCH_FLAGS, getCapabilities(), folderName);
        synchronized (getIoService()) {
            writeCommand(handler.buildCommand(uid, changedSince));
            String line;
            while (true) {
                line = ioReadLine();
                checkResponseNotBad(line);
                if (isTagged(line)) {
                    break;
                }
                handler.handleLine(line);
            }
            checkResponseOk(line);
        }
        return handler.getResult();
    }

    /**
     * Execute FETCH HEADERS command on CMS server
     * 
     * @param fromUid the from UID
     * @param toUid the to UID
     * @return An ordered collection of messages (containing only headers)
     * @throws ImapException
     * @throws IOException
     */
    public List<ImapMessage> fetchHeaders(Integer fromUid, Integer toUid) throws ImapException,
            IOException {

        FetchHeaderCmdHandler handler = (FetchHeaderCmdHandler) CmdHandler.getHandler(
                CommandType.FETCH_HEADERS, getCapabilities());
        synchronized (getIoService()) {
            writeCommand(handler.buildCommand(fromUid, toUid));
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
        return handler.getResult();
    }

    /**
     * Execute UID SEARCH command on CMS server
     *
     * @param headerName the header name
     * @param headerValue the header value
     * @return An ordered collection of uids
     * @throws ImapException
     * @throws IOException
     */
    public Integer searchUidWithHeader(String headerName, String headerValue) throws ImapException,
            IOException {

        UidSearchCmdHandler handler = (UidSearchCmdHandler) CmdHandler.getHandler(
                CommandType.UID_SEARCH, getCapabilities());
        synchronized (getIoService()) {
            writeCommand(handler.buildCommand("HEADER " + headerName + " " + headerValue));
            String line;
            while (true) {
                line = ioReadLine();
                checkResponseNotBad(line);
                if (isTagged(line)) {
                    break;
                }
                handler.handleLine(line);
            }
            checkResponseOk(line);
        }

        List<Integer> result = handler.getResult();
        return result.size() == 1 ? result.get(0) : null;
    }

    /**
     * Execute FETCH MESSAGE command on CMS server (one message)
     * 
     * @param uid the UID
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


    public synchronized int append(String folderName, List<Flag> flags, String payload)
            throws IOException, ImapException {
        // append INBOX (\Seen) {310}
        int length = payload.getBytes().length;

        if (!folderName.startsWith("\"")) {
            folderName = "\"" + folderName + "\"";
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
