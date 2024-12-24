package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.client.UDT_Timer;

import java.security.cert.CRL;
import java.util.WeakHashMap;

public class Sender_Window {
    private Client client;
    private int SenderWinSize = 8; //���ͷ����ڴ�С
    private int front =0;//���ʹ�����߽�
    private int rear=0;//β

    int offset=0;//ƫ��
    private TCP_PACKET[] packets = new TCP_PACKET[this.SenderWinSize];
    private UDT_Timer[] timer = new UDT_Timer[this.SenderWinSize];//ÿһ�����ݰ����м�ʱ�������ǲ���������ʽ�������Ӧ
    private boolean[] acked=new boolean[this.SenderWinSize];//��¼ÿ��λ���ǲ����а�

    private TaskPacketsRetrans task;
    public Sender_Window(Client client) {
        this.client = client;
    }
    public boolean isFull() {
        for(boolean i:acked){
            if(!i){
                return false;//��һ����λ��û��
            }
        }
        return true;
    }
    public void TakePacket(TCP_PACKET packet) {
        //����ʲôҲ����
        if (this.isFull()) {
            return;
        }
        //��β�������
        this.packets[this.rear]=packet;
        this.timer[this.rear]=new UDT_Timer();
        this.timer[this.rear].schedule(new UDT_RetransTask(this.client,packet),2000,3000);
        this.rear= (rear + 1) % SenderWinSize;
    }
    public void recvAck(int Current_Ack){
        int temp_ack=Current_Ack;//����ԭ����ack
        //����ƫ��������һ��
        Current_Ack=(Current_Ack-1)/100;
        Current_Ack-=offset;
        //�жϸ�AcK�ڷ��ʹ�����
        if(0<=Current_Ack&&Current_Ack<SenderWinSize){
            //���ж��ǲ����ظ������Ϳ�acked�������
            if(this.acked[Current_Ack]){
                return;
            }else {//�����õ�ack�ظ�
                this.acked[Current_Ack]=true;
                //ֹͣ��ʱ��
                this.timer[Current_Ack].cancel();
                this.timer[Current_Ack]=null;
                //ÿ����������жϴ����ܲ��ܻ���,������
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
