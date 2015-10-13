
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
import java.util.Set;

public class BasicImapService extends DefaultImapService {

    /**
     * @param ioService
     */
    public BasicImapService(IoService ioService) {
        super(ioService);
    }

    /**
     * @return List<ImapFolder>
     * @throws IOException
     * @throws ImapException
     */
    public List<ImapFolder> listStatus() throws IOException, ImapException {
        ListStatusCmdHandler handler = (ListStatusCmdHandler) CmdHandler
                .getHandler(CommandType.LIST_STATUS, getCapabilities());
        writeCommand(handler.buildCommand());
        handler.handleLines(readToEndOfResponse());
        return handler.getResult();
    }

    /**
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
     * @param folderName 
     * @param uid
     * @param changedSince
     * @return Set<FlagChange>
     * @throws IOException
     * @throws ImapException
     */
    public Set<FlagChange> fetchFlags(String folderName, Integer uid, Integer changedSince)
            throws IOException, ImapException {

        FetchFlagCmdHandler handler = (FetchFlagCmdHandler) CmdHandler
                .getHandler(CommandType.FETCH_FLAGS, getCapabilities(), folderName);
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
     * @param fromUid
     * @param toUid
     * @return An ordered collection of messages (containing only headers)
     * @throws ImapException
     * @throws IOException
     */
    public List<ImapMessage> fetchHeaders(Integer fromUid, Integer toUid)
            throws ImapException, IOException {

        FetchHeaderCmdHandler handler = (FetchHeaderCmdHandler) CmdHandler
                .getHandler(CommandType.FETCH_HEADERS, getCapabilities());
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
     * @param uid
     * @return ImapMessage
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
