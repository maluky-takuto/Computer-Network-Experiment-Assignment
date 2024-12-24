package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;
import java.util.TimerTask;

public class TaskPacketsRetrans extends TimerTask{
    private Client senderClient;
    private TCP_PACKET[] packets;
    public TaskPacketsRetrans(Client client, TCP_PACKET[] packets) {
        super();
        this.senderClient = client;
        this.packets = packets;
    }
    @Override
    public void run() {//·¢°ü
        for (TCP_PACKET packet : this.packets) {
            this.senderClient.send(packet);
        }
    }
}
