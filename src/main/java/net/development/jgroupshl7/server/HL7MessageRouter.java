package net.development.jgroupshl7.server;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.MllpComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageRouter {
    private static final Logger logger = LoggerFactory.getLogger(HL7MessageRouter.class);
    private static final String JGROUPS_CLUSTER_NAME = "HL7Cluster";
    private static final String JGROUPS_CONFIG_FILE = "kube.xml";
    private static final String HL7_SERVER_HOST = System.getenv("MY_POD_IP");
    private static final int HL7_SERVER_PORT = 3200;

    private JChannel channel;

    public void start() throws Exception {
        HL7MessageReceiver hl7receiver = new HL7MessageReceiver();
        channel = new JChannel(JGROUPS_CONFIG_FILE);
        channel.setReceiver(hl7receiver);
        channel.connect(JGROUPS_CLUSTER_NAME);

        // Set the local address in the receiver after connecting the channel
        hl7receiver.setLocalAddress(channel.getAddress());

        // Initialize Camel context and routes
        CamelContext camelContext = initCamelContext();

        // Start Camel context
        startCamelContext(camelContext);

        // Add shutdown hook to close resources properly
        addShutdownHook(camelContext);
    }

    private CamelContext initCamelContext() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();

        // Configure MLLP component
        MllpComponent mllpComponent = new MllpComponent();
        camelContext.addComponent("mllp", mllpComponent);

        // Add routes
        camelContext.addRoutes(createRouteBuilder());

        return camelContext;
    }

    private void startCamelContext(CamelContext camelContext) throws Exception {
        camelContext.start();
    }

    private void addShutdownHook(CamelContext camelContext) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                camelContext.stop();
                channel.close();
            } catch (Exception e) {
                logger.error("Error during shutdown: {}", e.getMessage(), e);
            }
        }));
    }

    private RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Route to receive HL7 messages over MLLP with auto acknowledgment
                from("mllp://" + HL7_SERVER_HOST + ":" + HL7_SERVER_PORT + "?autoAck=true")
                    .process(exchange -> {
                        String hl7Message = exchange.getIn().getBody(String.class);
                        logger.info("=**=>Incoming HL7 message:\n{}", hl7Message);
                        
                        // Log generic auto-generated ACK statement
                        logger.info("=**=>Sent MLLP_AUTO_ACKNOWLEDGEMENT message back to client");
                                                
                        // Forward HL7 message to JGroups cluster
                        forwardHL7MessageToCluster(hl7Message);
                    });
            }
        };
    }
   
    private void forwardHL7MessageToCluster(String hl7Message) {
        try {
            Message jgroupsMessage = new ObjectMessage(null, hl7Message);
            channel.send(jgroupsMessage);
        } catch (Exception e) {
            logger.error("Error forwarding HL7 message to cluster: {}", e.getMessage(), e);
        }
    }
    
}
