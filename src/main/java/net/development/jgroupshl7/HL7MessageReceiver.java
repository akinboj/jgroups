package net.development.jgroupshl7;

import java.io.UnsupportedEncodingException;

import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageReceiver implements Receiver {
	private static final Logger logger = LoggerFactory.getLogger(HL7MessageReceiver.class);
	
	public void receiveMessages() {
        // Wait for a few seconds to receive messages
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.error("Error waiting for messages: {}", e.getMessage(), e);
        }
    }

    @Override
    public void viewAccepted(View view) {
        logger.info("***Received view: {}", view);
    }

    @Override
    public void receive(Message msg) {
	        try {
	            String hl7Message = new String(msg.getPayload(), "UTF-8");
	            logger.info("***Received HL7 message: {}", hl7Message);
	        } catch (UnsupportedEncodingException e) {
	            logger.error("Error decoding HL7 message: {}", e.getMessage(), e);
	        }
    }

}
