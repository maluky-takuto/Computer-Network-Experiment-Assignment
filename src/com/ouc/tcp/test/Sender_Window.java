package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.client.UDT_Timer;

public class Sender_Window {
    private Client client;
    private int SenderWinSize = 64; //���ͷ����ڴ�С

    private mypacket[] packets=new mypacket[2*SenderWinSize];

    private int offset=0;

    public Sender_Window(Client client) {
        this.client = client;
    }

    public void init(){
        for(int i=0;i<=2*SenderWinSize-1;i++){
            this.packets[i]=new mypacket();
        }
    }

    public boolean isFull(){//�ǲ��Ƿ������ݰ�
        for(int i=0;i<=SenderWinSize-1;i++){
            if(this.packets[i].getPacket()==null){
                return false;
            }
        }
        return true;
    }

    public boolean isWait() {
        return this.packets[0].getPacket()!=null && !this.packets[0].isAcked();
    }


    public void TakePacket(TCP_PACKET packet) {
        //�жϰ��ڲ��ڴ���֮�ڣ������,���ʱ��
        int temp_seq=(packet.getTcpH().getTh_seq()-1)/100;
        temp_seq-=offset;
        if(0<=temp_seq && temp_seq<=SenderWinSize-1){
            UDT_Timer timer=new UDT_Timer();
            this.packets[temp_seq]=new mypacket(packet,timer);
            timer.schedule(new UDT_RetransTask(client,packet),3000,3000 );
        }
    }

    public void recvAck(int Current_Ack) {//��ack�ù���
        int temp_ack=(Current_Ack-1)/100;
        temp_ack-=offset;
        //�ڴ�����
        if(0<=temp_ack && temp_ack<=SenderWinSize-1){
            if(this.packets[temp_ack].getTimer()==null&&this.packets[temp_ack]!=null){
                return;//������ظ��İ������ļ�ʱ���Ѿ����ϴα�ͣ����
            }
            //��ȷ�յ��İ���
            this.packets[temp_ack].setAcked(true);//��ʾ�յ�
            this.packets[temp_ack].getTimer().cancel();//ȡ����ʱ��
            this.packets[temp_ack].setTimer(null);

            //ÿ�μӰ�֮�󣬽��д����ƶ�
            int baseline=0; //��ʾ��Ҫ�ƶ�����
            for(int i=0;i<=SenderWinSize-1;i++){
                if(this.packets[i].isAcked()){
                    baseline++;
                }else{
                    break;//û���ϣ��˳�
                }
            }
            //�ƶ�
            for(int i=0;i<=SenderWinSize-1;i++){
                this.packets[i]=this.packets[i+baseline];
            }
            offset+=baseline;//����ƫ����
        }
    }
}
