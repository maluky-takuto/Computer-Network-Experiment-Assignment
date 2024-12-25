package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;


public class Receiver_Window {
    private Client client;

    private int ReceiverWinSize = 64;
    private mypacket[] packets=new mypacket[2*ReceiverWinSize];

    private int offset=0;

    Queue<int[]> dataQueue = new LinkedBlockingQueue<>();
    public Receiver_Window(Client client) {
        this.client = client;
    }

    public void init(){
        for(int i=0;i<=2*ReceiverWinSize-1;i++){
            this.packets[i]=new mypacket();
        }
    }
//    public boolean isFull() {
//        for(mypacket temp:this.packets){
//            if(temp.isAcked()) {
//                return false;
//            }
//        }
//        return true;
//    }

    public int recvPacket(TCP_PACKET packet) {//收包
        int temp=packet.getTcpH().getTh_seq();
        int temp_seq= (packet.getTcpH().getTh_seq() - 1) / 100;
        temp_seq-=offset;
        //不在窗口内
        if(!(0<=temp_seq && temp_seq<=ReceiverWinSize-1)) {
            return temp;
        }else {//在窗口
            this.packets[temp_seq] = new mypacket(packet);
            this.packets[temp_seq].setAcked(true);//表示收到
            this.dataQueue.add(packet.getTcpS().getData());//加入缓存队列

            int baseline = 0;
            for (int i = 0; i <= ReceiverWinSize - 1; i++) {
                if (this.packets[i].isAcked()) {
                    baseline++;
                }
                break;
            }
            //移动,并缓存
            for (int i = 0; i <= ReceiverWinSize-1; i++) {
                this.packets[i]= this.packets[i + baseline];
            }
            offset += baseline;
            if(this.dataQueue.size()>=20||this.offset>=1000){
                this.deliver_data();
            }
            return temp;
        }
    }

    public void deliver_data() {
        // 检查 this.dataQueue，将数据写入文件
        try {
            File file = new File("recvData.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

            while (!this.dataQueue.isEmpty()) {
                int[] data = this.dataQueue.poll();

                // 将数据写入文件
                for (int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();  // 清空输出缓存
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
