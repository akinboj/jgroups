package au.gov.act.health.interfaces.jgroups;

import org.jgroups.Receiver;
import org.jgroups.View;

public class SendReceiveChat implements Receiver {
    
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    public void receive() {
        ChatEventLoop chatloop = new ChatEventLoop();
        System.out.println(chatloop.message.getSrc() + ": " + chatloop.message.getObject());
    }

}
