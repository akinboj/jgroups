package net.development.jgroupshl7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(HL7MessageSender.class);
    private static final String JGROUPS_CLUSTER_NAME = "HL7Cluster";
    private static final String JGROUPS_CONFIG_FILE = "tcp.xml";
    private static final String HL7_SERVER_HOST = "192.168.0.17";
    private static final int HL7_SERVER_PORT = 3200;

    private JChannel channel;
    private ServerSocket serverSocket;

    public void start() throws Exception {
        HL7MessageReceiver hl7receiver = new HL7MessageReceiver();
        channel = new JChannel(JGROUPS_CONFIG_FILE);
        channel.setReceiver(hl7receiver);
        channel.connect(JGROUPS_CLUSTER_NAME);

        // Start the HL7 message server
        startHL7MessageServer();

        hl7receiver.receiveMessages();
        channel.close();
        serverSocket.close();
    }

    private void startHL7MessageServer() throws InterruptedException {
        try {
            serverSocket = new ServerSocket(HL7_SERVER_PORT, 1, java.net.InetAddress.getByName(HL7_SERVER_HOST));
            logger.info("HL7 message server started on {}:{}", HL7_SERVER_HOST, HL7_SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleHL7Message(clientSocket);
            }
        } catch (IOException e) {
            logger.error("Error starting HL7 message server: {}", e.getMessage(), e);
        }
    }
    
    private void handleHL7Message(Socket clientSocket) throws InterruptedException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            StringBuilder hl7MessageBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                hl7MessageBuilder.append(line).append("\n");
                if (line.equals("" + (char) 28)) {
                    break;
                }
            }
            String hl7Message = hl7MessageBuilder.toString();
            logger.info("=**=>Incoming HL7 message:\n{}", hl7Message);

            // Send the HL7 message through the JGroups channel
            sendHL7Message(hl7Message);

            // Send the ACK back to the client using the MLLP adapter
            MLLPAdapter.sendACK(clientSocket, hl7Message);
        } catch (IOException e) {
            logger.error("Error handling HL7 message: {}", e.getMessage(), e);
        }
    } 
    
    private void sendHL7Message(String hl7Message) throws InterruptedException {
        try {
            Message msg = new ObjectMessage(null, hl7Message.getBytes("UTF-8"));
            channel.send(msg);
        } catch (Exception e) {
            logger.error("Error sending HL7 message: {}", e.getMessage(), e);
        }
    }
}
