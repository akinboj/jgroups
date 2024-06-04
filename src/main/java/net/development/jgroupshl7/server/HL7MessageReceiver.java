package net.development.jgroupshl7.server;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageReceiver implements Receiver {
    private static final Logger logger = LoggerFactory.getLogger(HL7MessageReceiver.class);
    private Address localAddress;
    private Set<Address> previousMembers = new HashSet<>();

    // No-argument constructor
    public HL7MessageReceiver() {
    }

    public void setLocalAddress(Address localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public void receive(Message msg) {
        // Log message sender and local address
        logger.info("Local address is set to: {}", localAddress);

        // Ignore the message if it was sent by the local node
        if (msg.getSrc().equals(localAddress)) {
            logger.info("***Ignoring message sent by the local node.");
            return;
        }

        try {
            // Retrieve the object from the message
            Object obj = msg.getObject();

            // Check if the object is a byte array and convert it to a string
            String hl7Message;
            if (obj instanceof byte[]) {
                byte[] buffer = (byte[]) obj;
                hl7Message = new String(buffer, StandardCharsets.UTF_8);
            } else if (obj instanceof String) {
                hl7Message = (String) obj;
            } else {
                logger.error("Received an unsupported message type: {}", obj.getClass().getName());
                return;
            }

            // Get the sender address
            String senderAddress = msg.getSrc().toString();
            logger.info("=**=>Received Forwarded HL7 message from JGroups member::[{}]\n{}", senderAddress, hl7Message);
            // Handle the received HL7 message
        } catch (Exception e) {
            logger.error("Error receiving HL7 message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void viewAccepted(View view) {
        List<Address> members = view.getMembers();
        if (members.isEmpty()) {
            logger.info("No members in the cluster.");
        } else {
            for (Address member : members) {
                if (!member.equals(localAddress) && !previousMembers.contains(member)) {
                    logger.info(">>> Member: [{}] JOINED the cluster", member);
                }
            }
            for (Address member : previousMembers) {
                if (!members.contains(member)) {
                    logger.info("<<< Member: [{}] LEFT the cluster", member);
                }
            }
            previousMembers.clear();
            previousMembers.addAll(members);
            logger.info("*** Received view: {}", view);
        }
    }

}