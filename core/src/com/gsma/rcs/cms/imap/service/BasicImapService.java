/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms.imap.service;

import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.cmd.CmdHandler;
import com.gsma.rcs.cms.imap.cmd.CmdHandler.CommandType;
import com.gsma.rcs.cms.imap.cmd.FetchFlagCmdHandler;
import com.gsma.rcs.cms.imap.cmd.FetchHeaderCmdHandler;
import com.gsma.rcs.cms.imap.cmd.ListStatusCmdHandler;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import com.sonymobile.rcs.imap.DefaultImapService;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.IoService;

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
     * Execute LIST-STATUS command on CMS server
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
     * Execute FETCH FLAGS command on CMS server
     * @param folderName
     * @param uid
     * @param changedSince
     * @return
     * @throws IOException
     * @throws ImapException
     */
    public List<FlagChange> fetchFlags(String folderName, Integer uid, Integer changedSince)
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
     * @param fromUid
     * @param toUid
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
     * Execute FETCH MESSAGE command on CMS server (one message)
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
}
