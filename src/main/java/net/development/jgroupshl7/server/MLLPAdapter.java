package net.development.jgroupshl7.server;

public class MLLPAdapter { 
	
    public static String generateACKMessage(String hl7Message) {
        DateFormatter hl7DateTime = new DateFormatter();
        int newlineIndex = hl7Message.indexOf("\n");
        
        if (newlineIndex < 0 || newlineIndex >= hl7Message.length()) {
            newlineIndex = hl7Message.length();
        }

        String mshSegment = hl7Message.substring(0, newlineIndex);
        String[] mshFields = mshSegment.split("\\|");

        StringBuilder ackMessage = new StringBuilder();
        ackMessage.append((char) 0x0B); // Start of block
        ackMessage.append("\nMSH|^~\\&|").append(mshFields[3]).append("|").append(mshFields[4])
                   .append("|").append(mshFields[5]).append("||").append(hl7DateTime.hl7AckTimeFormat())
                   .append("|").append("ACK^").append(mshFields[9]).append("|").append(mshFields[10])
                   .append("|P|2.3|\r")
                   .append("\nMSA|AA|").append(mshFields[9]).append("\r"); // <--- Check this line
        ackMessage.append((char) 0x1C); // End of block
        ackMessage.append((char) 0x0D); // Carriage return
        return ackMessage.toString();
    }
}
