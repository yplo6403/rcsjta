package com.gsma.rcs.cms.imap.task;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;
import com.gsma.rcs.cms.imap.message.sms.ImapSmsMessage;
import com.gsma.rcs.cms.imap.service.BasicImapService;
import com.gsma.rcs.cms.provider.xms.XmsLog;
import com.gsma.rcs.cms.provider.xms.model.SmsData;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.PushStatus;
import com.gsma.rcs.cms.provider.xms.model.AbstractXmsData.ReadStatus;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.ImapException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushMessageTaskMock extends PushMessageTask {

    public PushMessageTaskMock(BasicImapService imapService, XmsLog xmsLog, String myNumber,
            PushMessageTaskListener callback) {
        super(imapService, xmsLog, myNumber, callback);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public Map<Long, Integer> pushMessages(List<SmsData> messages) {
        String from, to, direction;
        from = to = direction = null;

        Map<Long, Integer> createdUids = new HashMap<Long, Integer>();
        try {
            List<String> existingFolders = new ArrayList<String>();
            for (ImapFolder imapFolder : mImapService.listStatus()) {
                existingFolders.add(imapFolder.getName());
            }
            for (SmsData message : messages) {
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

                String remoteFolder = Constants.TEL_PREFIX.concat(message.getContact());
                if (!existingFolders.contains(remoteFolder)) {
                    mImapService.create(remoteFolder);
                    existingFolders.add(remoteFolder);
                }
                mImapService.selectCondstore(remoteFolder);
                int uid = mImapService.append(remoteFolder, flags,
                        imapSmsMessage.getPart());
                createdUids.put(message.getNativeProviderId(), uid);
            }            
        } catch (IOException | ImapException e) {
            e.printStackTrace();
        }
        return createdUids;
    }
}
