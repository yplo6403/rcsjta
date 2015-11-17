package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.fordemo.ImapContext;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.provider.imap.MessageData;
import com.gsma.rcs.cms.provider.xms.PartLog;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.XmsData;
import com.gsma.rcs.cms.provider.xms.model.XmsData.ReadStatus;

import com.gsma.rcs.cms.utils.CmsUtils;
import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PushMessageTaskMock extends PushMessageTask {

    public PushMessageTaskMock(BasicImapService imapService, XmsLog xmsLog, PartLog partLog, String myNumber, ImapContext imapContext,
            PushMessageTaskListener callback) {
        super(imapService, xmsLog, partLog, myNumber, imapContext, callback);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public Boolean pushMessages(List<XmsData> messages) {
        String from, to, direction;
        from = to = direction = null;

        try {
            List<String> existingFolders = new ArrayList<String>();
            for (ImapFolder imapFolder : mImapService.listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            for (XmsData message : messages) {
                List<Flag> flags = new ArrayList<Flag>();
                switch (message.getDirection()) {
                    case INCOMING:
                        from = message.getContact();
                        to = mMyNumber;
                        direction = Constants.DIRECTION_RECEIVED;
                        break;
                    case OUTGOING:
                        from = mMyNumber;
                        to = message.getContact();
                        direction = Constants.DIRECTION_SENT;
                        break;
                    default:
                        break;
                }
                
                if (message.getReadStatus() != ReadStatus.UNREAD) {
                    flags.add(Flag.Seen);
                }

                ImapSmsMessage imapSmsMessage = new ImapSmsMessage(from, to, direction,
                        message.getDate(), message.getContent(), "" + message.getDate(),
                        "" + message.getDate(), "" + message.getDate());

                String remoteFolder = CmsUtils.convertContactToCmsRemoteFolder(MessageData.MessageType.SMS, message.getContact());
                if (!existingFolders.contains(remoteFolder)) {
                    mImapService.create(remoteFolder);
                    existingFolders.add(remoteFolder);
                }
                mImapService.selectCondstore(remoteFolder);
                int uid = mImapService.append(remoteFolder, flags,
                        imapSmsMessage.getPart());
                mImapContext.addNewEntry(remoteFolder, uid, message.getBaseId());
            }            
        } catch (IOException | ImapException e) {
            e.printStackTrace();
        }
        return true ;
    }
}
