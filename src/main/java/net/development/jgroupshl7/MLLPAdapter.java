package net.development.jgroupshl7;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MLLPAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MLLPAdapter.class);

    public static void sendACK(Socket clientSocket, String hl7Message) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            // Construct the ACK message
            String ackMessage = constructACKMessage(hl7Message);
            out.write(ackMessage);
            out.flush();
            logger.info("Sent ACK message to client: {}", ackMessage);
        } catch (IOException e) {
            logger.error("Error sending ACK: {}", e.getMessage(), e);
        }
    }
    
    private static String constructACKMessage(String hl7Message) {
    	DateFormatter hl7DateTime = new DateFormatter();
        String mshSegment = hl7Message.substring(0, hl7Message.indexOf("\n"));
        String[] mshFields = mshSegment.split("\\|");

        StringBuilder ackMessage = new StringBuilder();
        ackMessage.append("MSH|^~\\&|").append(mshFields[3]).append("|").append(mshFields[4])
                   .append("|").append(mshFields[5]).append("||").append(hl7DateTime.hl7AckTimeFormat())
                   .append("||ACK^").append(mshFields[9]).append("|").append(mshFields[10])
                   .append("|P|2.3|\r\n")
                   .append("MSA|AA|").append(mshFields[10]).append("\r");
        return ackMessage.toString();
    }
    
    
}

