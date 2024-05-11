package au.gov.act.health.interfaces.jgroups;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;

public class ChatEventLoop {
    String user_name=System.getProperty("user.name", "n/a");
    JChannel channel;
    Message message;
    
    @SuppressWarnings("resource")
    public void start() throws Exception {
        SendReceiveChat chatflow = new SendReceiveChat();
        channel=new JChannel("src/main/resources/tcp.xml").setReceiver(chatflow).connect("ChatCluster");
        channel.connect("ChatCluster");
        eventLoop();
        channel.close();
    }
    
    public void eventLoop() {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                System.out.print("> "); System.out.flush();
                String line=in.readLine().toLowerCase();
                if(line.startsWith("quit") || line.startsWith("exit"))
                    break;
                line="[" + user_name + "] " + line;
                Message message=new ObjectMessage(null, line);
                channel.send(message);
            }
            catch(Exception e) {
            }
        }
    }

}
