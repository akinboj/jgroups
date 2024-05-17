package net.development.jgroupshl7.server;

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

}
