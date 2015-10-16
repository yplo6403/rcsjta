//package com.gsma.rcs.cms.storage;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class SmsStorageHandlerMock extends SmsMemoryStorageHandler {
//
//    public static List<String> getMessages(){
//        List<String> messages = new ArrayList<String>();
//        for(SmsMessage sms : smsStorage.values()){
//            messages.add(sms.toString());
//        }
//        return messages;
//    }
//
//    public static void clear(){       
//        smsStorage.clear();
//    }
//}
