package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Receiver_Window {
    private Client client;
    private int ReceiverWinSize = 8;
    private int left =0;//���
    private TCP_PACKET[] packets = new TCP_PACKET[this.ReceiverWinSize];
    Queue<int[]> dataQueue = new LinkedBlockingQueue<>();
    private int counts = 0;////////////////
    public Receiver_Window(Client client) {
        this.client = client;
    }
    public int recvPacket(TCP_PACKET packet) {//�հ�

        return 0;
    }

private void slide() { //��������

}

    public void deliverdata() {

    }//��� this.dataQueue,������д���ļ�
}
