package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.client.UDT_Timer;

import java.security.cert.CRL;
import java.util.WeakHashMap;

public class Sender_Window {
    private Client client;
    private int SenderWinSize = 8; //发送方窗口大小
    private int front =0;//发送窗口左边界
    private int rear=0;//尾

    int offset=0;//偏移
    private TCP_PACKET[] packets = new TCP_PACKET[this.SenderWinSize];
    private UDT_Timer[] timer = new UDT_Timer[this.SenderWinSize];//每一个数据包都有计时器，还是采用数组形式，方便对应
    private boolean[] acked=new boolean[this.SenderWinSize];//记录每个位置是不是有包

    private TaskPacketsRetrans task;
    public Sender_Window(Client client) {
        this.client = client;
    }
    public boolean isFull() {
        for(boolean i:acked){
            if(!i){
                return false;//有一个空位就没满
            }
        }
        return true;
    }
    public void TakePacket(TCP_PACKET packet) {
        //满了什么也不做
        if (this.isFull()) {
            return;
        }
        //队尾，加入包
        this.packets[this.rear]=packet;
        this.timer[this.rear]=new UDT_Timer();
        this.timer[this.rear].schedule(new UDT_RetransTask(this.client,packet),2000,3000);
        this.rear= (rear + 1) % SenderWinSize;
    }
    public void recvAck(int Current_Ack){
        int temp_ack=Current_Ack;//保留原来的ack
        //先用偏移量处理一下
        Current_Ack=(Current_Ack-1)/100;
        Current_Ack-=offset;
        //判断该AcK在发送窗口内
        if(0<=Current_Ack&&Current_Ack<SenderWinSize){
            //再判断是不是重复包，就看acked数组就行
            if(this.acked[Current_Ack]){
                return;
            }else {//是有用的ack回复
                this.acked[Current_Ack]=true;
                //停止计时器
                this.timer[Current_Ack].cancel();
                this.timer[Current_Ack]=null;
                //每次收完包，判断窗口能不能滑动,即出队
                while(this.acked[this.front]){
                    this.packets[this.front]=null;
                    this.timer[this.front]=null;
                    this.acked[this.front]=false;
                    front = (front + 1) % SenderWinSize;
                }
            }
        }
    }
}
