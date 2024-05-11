package net.development.jgroupshl7;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageSender {
	private static final Logger logger = LoggerFactory.getLogger(HL7MessageSender.class);
	private static final String JGROUPS_CLUSTER_NAME = "HL7Cluster";
    private static final String JGROUPS_CONFIG_FILE = "tcp.xml";
    
    private JChannel channel;
    
    public void start() throws Exception {
    	HL7MessageReceiver hl7receiver = new HL7MessageReceiver();
        channel = new JChannel(JGROUPS_CONFIG_FILE);
        channel.setReceiver(hl7receiver);
        channel.connect(JGROUPS_CLUSTER_NAME);
        sendHL7Message(getHL7Message());
        hl7receiver.receiveMessages();
        channel.close();
    }
    
    private String getHL7Message() {
        // Construct a sample HL7 message
        return "MSH|^~\\&|SENDER|FACILITY|RECEIVER|FACILITY|20230501120000||ADT^A01|MSG001|P|2.3\r" +
               "PID|1|123456789|123456789^^^MRN|||||M\r" +
               "PV1|1|I|ROOM1^BED1||||||||||||||20230501|20230502";
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

}
