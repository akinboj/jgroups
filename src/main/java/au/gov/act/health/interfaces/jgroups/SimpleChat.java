package au.gov.act.health.interfaces.jgroups;

public class SimpleChat {

    public static void main(String[] args) throws Exception {
        ChatEventLoop chatmain = new ChatEventLoop();
        chatmain.start();

    }

}
