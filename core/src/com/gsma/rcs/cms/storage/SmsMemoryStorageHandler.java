//
//package com.gsma.rcs.cms.storage;
//
//import com.gsma.rcs.cms.Constants;
//import com.gsma.rcs.cms.imap.message.IImapMessage;
//
//import com.sonymobile.rcs.imap.Flag;
//import com.sonymobile.rcs.imap.ImapMessage;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//public class SmsMemoryStorageHandler implements IMessageHandler {
//
//    protected static Map<String, SmsMessage> smsStorage = new LinkedHashMap<String, SmsMessage>();
//
//    /**
//     * Default constructor
//     */
//    public SmsMemoryStorageHandler() {
//    }
//
//    /**
//     * @param message
//     * @return messageId
//     */
//    public String createNewMessage(ImapMessage message) {
//        SmsMessage smsMessage = new SmsMessage(message);
//        String messageId = smsMessage.getMessageId();
//        smsStorage.put(messageId, smsMessage);
//        return messageId;
//    }
//
//    public class SmsMessage {
//
//        private IImapMessage mImapMessage;
//
//        public SmsMessage(IImapMessage imapMessage) {
//            mImapMessage = imapMessage;
//        }
//
//        public void markAsSeen() {
//            mImapMessage.getMetadata().getFlags().add(Flag.Seen);
//        }
//
//        public void markAsDeleted() {
//            mImapMessage.getMetadata().getFlags().add(Flag.Deleted);
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj)
//                return true;
//            if (obj == null)
//                return false;
//            if (getClass() != obj.getClass())
//                return false;
//            SmsMessage other = (SmsMessage) obj;
//            if (mImapMessage == null) {
//                if (other.mImapMessage != null)
//                    return false;
//            } else if (!mImapMessage.getFrom().equals(other.mImapMessage.getFrom())
//                    || !mImapMessage.getBody().getHeader(Constants.HEADER_TO)
//                            .equals(other.mImapMessage.getBody().getHeader(Constants.HEADER_TO))
//                    || !mImapMessage.getBody().getHeader(Constants.HEADER_MESSAGE_CORRELATOR)
//                            .equals(other.mImapMessage.getBody()
//                                    .getHeader(Constants.HEADER_MESSAGE_CORRELATOR)))
//                return false;
//            return true;
//        }
//
//        public String getMessageId() {
//            return new StringBuffer(mImapMessage.getFrom())
//                    .append(mImapMessage.getBody().getHeader("To"))
//                    .append(mImapMessage.getBody().getHeader("Message-Correlator")).toString();
//        }
//
//        public String toString() {
//            return new StringBuilder(mImapMessage.getFrom()).append(":")
//                    .append(mImapMessage.getTextBody()).append(":").append("\r\n").append(mImapMessage.getMetadata().getFlags()).toString();
//        }
//    }
//
//    @Override
//    public void onReadFlagEvent(String messageId) {
//        smsStorage.get(messageId).markAsSeen();
//    }
//
//    @Override
//    public void onDeletedFlagEvent(String messageId) {
//        smsStorage.get(messageId).markAsDeleted();;        
//    }
//
//    @Override
//    public String getMessageId(IImapMessage message) {
//        String messageId = null;
//        SmsMessage sms = new SmsMessage(message);
//        if(smsStorage.containsKey(sms.getMessageId())){
//            messageId = sms.getMessageId();
//        }
//        return messageId;
//    }
//}
