package net.development.jgroupshl7.server;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.apache.camel.component.mllp.MllpComponent;
import org.apache.camel.component.mllp.MllpConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.component.rabbitmq.RabbitMQComponent;
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
    private static final String RABBITMQ_QUEUE_NAME = "hl7-messages";
    private static final String RABBITMQ_HOST = System.getenv("RABBITMQ_HOST");
    private static final String RABBITMQ_PORT = System.getenv("RABBITMQ_PORT");
    private static final String RABBITMQ_USERNAME = System.getenv("RABBITMQ_USERNAME");
    private static final String RABBITMQ_PASSWORD = System.getenv("RABBITMQ_PASSWORD");
    private static final String RABBITMQ_URI = "rabbitmq://" + RABBITMQ_HOST + ":" + RABBITMQ_PORT + "/" + RABBITMQ_QUEUE_NAME
            + "?username=" + RABBITMQ_USERNAME
            + "&password=" + RABBITMQ_PASSWORD
            + "&queue=" + RABBITMQ_QUEUE_NAME
            + "&autoDelete=false"
            + "&durable=true"
            + "&declare=true";

    private JChannel channel;
    private CamelContext camelContext;
    private ProducerTemplate producerTemplate;

    public void start() throws Exception {
        HL7MessageReceiver hl7receiver = new HL7MessageReceiver();
        channel = new JChannel(JGROUPS_CONFIG_FILE);
        channel.setReceiver(hl7receiver);
        channel.connect(JGROUPS_CLUSTER_NAME);

        // Set the local address in the receiver after connecting the channel
        hl7receiver.setLocalAddress(channel.getAddress());

        // Initialize Camel context and routes
        camelContext = initCamelContext();

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

        // Configure RabbitMQ component
        RabbitMQComponent rabbitMQComponent = new RabbitMQComponent();
        camelContext.addComponent("rabbitmq", rabbitMQComponent);

        // Add routes
        camelContext.addRoutes(createRouteBuilder());

        // Create a single ProducerTemplate instance for RabbitMQ
        producerTemplate = camelContext.createProducerTemplate();

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
                // Route to receive HL7 messages over MLLP with auto acknowledgment disabled
                from("mllp://" + HL7_SERVER_HOST + ":" + HL7_SERVER_PORT + "?autoAck=true")
                    .unmarshal(new HL7DataFormat()) // Unmarshal HL7 messages
                    .process(exchange -> {
                        String hl7Message = exchange.getIn().getBody(String.class);
                        logger.info("=**=> Incoming HL7 message:\n{}", hl7Message);

                        // Forward HL7 message to JGroups cluster
                        forwardHL7MessageToCluster(hl7Message);

                        // Publish HL7 message to RabbitMQ
                        sendHL7MessageToRabbitMQ(hl7Message, exchange);
                    })
                    .transform(HL7.ack()) // Generate and transform to ACK message
                    .process(exchange -> {
                        // Retrieve and log the generated ACK message
                        String ackMessage = exchange.getMessage().getBody(String.class);
                        logger.info("=**=> Auto-generated ACK:\n{}", ackMessage);

                        // Set the ACK message in the exchange properties
                        exchange.getMessage().setHeader(MllpConstants.MLLP_ACKNOWLEDGEMENT, ackMessage);
                    })
                    .marshal(new HL7DataFormat()); // Marshal ACK messages back to HL7 format
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

    private void sendHL7MessageToRabbitMQ(String hl7Message, org.apache.camel.Exchange exchange) {
        try {
            producerTemplate.sendBody(RABBITMQ_URI, hl7Message);
            String msgControlID = exchange.getIn().getHeader(MllpConstants.MLLP_MESSAGE_CONTROL, String.class);
            logger.info("=*=*>Message with MSH-10: {} published to queue", msgControlID);
        } catch (Exception e) {
            logger.error("Error sending HL7 message to RabbitMQ: {}", e.getMessage(), e);
        }
    }
}
