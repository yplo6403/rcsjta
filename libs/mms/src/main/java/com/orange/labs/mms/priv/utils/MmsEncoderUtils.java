/*
 *
 * MmsLib
 * 
 * Module name: com.orange.labs.mms
 * Version:     2.0
 * Created:     2013-06-06
 * Author:      vkxr8185 (Yoann Hamon)
 * 
 * Copyright (C) 2013 Orange
 *
 * This software is confidential and proprietary information of France Telecom Orange.
 * You shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 */

package com.orange.labs.mms.priv.utils;

import com.orange.labs.mms.MmsMessage;
import com.orange.labs.mms.priv.MmsException;
import com.orange.labs.mms.priv.PartMMS;
import com.orange.labs.mms.priv.parser.MmsEncodedMessage;
import com.orange.labs.mms.priv.parser.MmsEncodedPart;
import com.orange.labs.mms.priv.parser.MmsEncoder;

import java.util.Random;

public final class MmsEncoderUtils
{
	public static byte[] encodeMessage(MmsMessage msg) throws MmsException
	{
		// Create a new message
		MmsEncodedMessage mms = new MmsEncodedMessage();
	    Random r = new Random();

    	//Set the sender and receiver addresses
	    mms.setFrom("0611223344");
	    mms.setTo(msg.getTo());
        
        // Set a random transaction id
	    mms.setTransactionId(String.valueOf(r.nextInt(999999999)));
        
        // Set the date of the mms
	    mms.setDate(System.currentTimeMillis());
        
        // Set the subject of the message
	    mms.setSubject(msg.getSubject());
        
        // Set the content type
	    mms.setContentType("application/vnd.wap.multipart.mixed");
        
        // Set the message type
	    mms.setType("m-send-req");

        // Create new parts that will contain the images
        for (PartMMS part : msg.getParts())
        {
	        MmsEncodedPart encPart = new MmsEncodedPart();
	        encPart.setContentType(part.getMimeType());
	        encPart.setContent(part.getContent());
	
	        // Link the part to the message
	        mms.addPart(encPart);
        }

        // Encode the message
        MmsEncoder encoder = new MmsEncoder(mms);
        
        // Return the encoded message as bytes array
        return encoder.encode();
	}
}
