package net.development.jgroupshl7;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class JGroupsMessageSenderReceiver implements Receiver {
    private static final Logger logger = LoggerFactory.getLogger(JGroupsMessageSenderReceiver.class);
    private static final String JGROUPS_CLUSTER_NAME = "HL7Cluster";
    private static final String JGROUPS_CONFIG_FILE = "src/main/resources/tcp.xml";

    private JChannel channel;

    public void start() throws Exception {
        channel = new JChannel(JGROUPS_CONFIG_FILE);
        channel.setReceiver(this);
        channel.connect(JGROUPS_CLUSTER_NAME);
        sendHL7Message(getHL7Message());
        receiveMessages();
        channel.close();
    }

    private void sendHL7Message(String hl7Message) throws InterruptedException {
    	while(true) {
    		Thread.sleep(5000);
	        try {
	            Message msg = new ObjectMessage(null, hl7Message.getBytes("UTF-8"));
	            channel.send(msg);
	            logger.info("***HL7 message sent: {}", hl7Message);
	        } catch (Exception e) {
	            logger.error("Error sending HL7 message: {}", e.getMessage(), e);
	        }
    	}
    }

    private void receiveMessages() {
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

    private String getHL7Message() {
        // Construct a sample HL7 message
        return "MSH|^~\\&|SENDER|FACILITY|RECEIVER|FACILITY|20230501120000||ADT^A01|MSG001|P|2.3\r" +
               "PID|1|123456789|123456789^^^MRN|||||M\r" +
               "PV1|1|I|ROOM1^BED1||||||||||||||20230501|20230502";
    }

    public static void main(String[] args) throws Exception {
    	System.setProperty("jgroups.bind_addr", "192.168.0.17");
    	System.setProperty("jgroups.tcpping.initial_hosts", "192.168.0.17[7800],192.168.0.17[7801]");
        
    	new JGroupsMessageSenderReceiver().start();
    }
}
