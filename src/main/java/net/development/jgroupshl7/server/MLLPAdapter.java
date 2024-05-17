package net.development.jgroupshl7.server;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MLLPAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MLLPAdapter.class);
    
    public static void sendACK(Socket clientSocket, String hl7Message) {
        try {
            int timeout = 5000; // 5 seconds
            clientSocket.setSoTimeout(timeout);

            // Construct the MLLP-wrapped ACK message
            String ackMessage = generateACKMessage(hl7Message);

            // Split the ACK message into MSH and MSA segments
            String[] ackSegments = ackMessage.split("\r");

            // Write the MLLP-wrapped ACK message to the client socket
            clientSocket.getOutputStream().write(ackMessage.getBytes());
            clientSocket.getOutputStream().flush();

            // Log the ACK message in two separate lines
            logger.info("=**=>Sent ACK message to client: ");
            logger.info("{}", ackSegments[0]);
            logger.info("{}", ackSegments[1]);
        } catch (SocketTimeoutException e) {
            logger.error("Timeout occurred while sending ACK message: {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error sending ACK: {}", e.getMessage(), e);
        }
    }

    
    private static String generateACKMessage(String hl7Message) {
    	DateFormatter hl7DateTime = new DateFormatter();
        String mshSegment = hl7Message.substring(0, hl7Message.indexOf("\n"));
        String[] mshFields = mshSegment.split("\\|");

        StringBuilder ackMessage = new StringBuilder();
        ackMessage.append((char) 0x0B); // Start of block
        ackMessage.append("MSH|^~\\&|").append(mshFields[3]).append("|").append(mshFields[4])
                   .append("|").append(mshFields[5]).append("||").append(hl7DateTime.hl7AckTimeFormat())
                   .append("||ACK^").append(mshFields[9]).append("|").append(mshFields[10])
                   .append("|P|2.3|\r")
                   .append("MSA|AA|").append(mshFields[10]).append("\r");
        ackMessage.append((char) 0x1C); // End of block
        ackMessage.append((char) 0x0D); // Carriage return
        return ackMessage.toString();
    }
    
}

