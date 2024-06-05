package net.development.jgroupshl7.server;

public class HL7StartInterface {
	
	public static void main(String[] args) throws Exception {
		
		HL7MessageRouter cluster = new HL7MessageRouter();
    	cluster.start();
    }

}