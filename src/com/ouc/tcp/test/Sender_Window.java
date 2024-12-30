package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.client.UDT_Timer;

import javax.swing.undo.CannotUndoException;
import java.util.Hashtable;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

public class Sender_Window {
    private Client client;
    private int SenderWinSize = 64; //���ͷ����ڴ�С

    //private mypacket[] packets=new mypacket[2*SenderWinSize];

    private int offset=0;

    //����Tahoe,reno����ı���
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>(); // �洢�����ڵ����ݰ�
    private Hashtable<Integer, UDT_Timer> timer = new Hashtable<>(); // �洢ÿ�����ݰ��ļ�ʱ��
    private volatile int ssthresh = 16;  //����ֵ
    public int cwnd = 1;  //ӵ������
    private int front_ack_seq = -1; //��һ���յ�ACK����seq
    private int repeat_num = 0; //�ظ���Ack��
    private int count_ack = 0; // ����ӵ������״̬ʱ�յ���ACK������¼һ��RTT�յ�ACK�Ľ���


    public Sender_Window(Client client) {
        this.client = client;
    }

//    public void init(){
//        for(int i=0;i<=2*SenderWinSize-1;i++){
//            this.packets[i]=new mypacket();
//        }
//    }

    public boolean isFull(){//�ǲ��Ƿ������ݰ�
//        for(int i=0;i<=SenderWinSize-1;i++){
//            if(this.packets[i].getPacket()==null){
//                return false;
//            }
//        }
//        return true;
        return this.cwnd<=packets.size();
    }

//    public boolean isWait() {
//        return this.packets[0].getPacket()!=null && !this.packets[0].isAcked();
//    }


    public void TakePacket(TCP_PACKET packet) {
        //�жϰ��ڲ��ڴ���֮�ڣ������,���ʱ��
//        int temp_seq=(packet.getTcpH().getTh_seq()-1)/100;
//        temp_seq-=offset;
//        if(0<=temp_seq && temp_seq<=SenderWinSize-1){
//            UDT_Timer timer=new UDT_Timer();
//            this.packets[temp_seq]=new mypacket(packet,timer);
//            timer.schedule(new UDT_RetransTask(client,packet),3000,3000 );
        int Current_seq=(packet.getTcpH().getTh_seq()-1)/100;
        timer.put(Current_seq,new UDT_Timer());
        timer.get(Current_seq).schedule(new TahoeRetran(client,packet),3000,3000);
        packets.put(Current_seq,packet);
        }

    public void recvAck(int CurSeq) {//��ack�ù���
        /*int temp_ack=(Current_Ack-1)/100;
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
        }*/
        if (CurSeq != front_ack_seq) { //�µ�����ACK��
            for (int i = front_ack_seq + 1; i <= CurSeq; i++) {
                packets.remove(i);
                if (timer.containsKey(i)) {
                    timer.get(i).cancel();
                    timer.remove(i);
                }
            }
            front_ack_seq = CurSeq;
            repeat_num = 0;
            if (cwnd < ssthresh) { //����ʼ�㷨
                System.out.println("***********����ʼ***********");
                System.out.println(cwnd + "->" + (cwnd + 1));
                System.out.println();
                cwnd++;
            } else { //ӵ������
                count_ack++;
                System.out.println("***********ӵ������***********");
                System.out.println("cwnd:" + cwnd + "  RTT����" + count_ack);
                // ���һ��RTT��ACK�������� cwnd
                if (count_ack >= cwnd) {
                    count_ack -= cwnd;//����RTT����
                    System.out.println("�ӷ�����:" + cwnd + "->" + (cwnd + 1));
                    System.out.println();
                    cwnd++;
                }
            }
        } else { //������ظ���
            repeat_num++;
            if(repeat_num >= 3){ //�ظ�3�Σ�Ҫִ�п�ָ�
                TCP_PACKET packet = packets.get(CurSeq + 1);
                if (packet != null) {
                    System.out.println("***********��ָ�***********");
                    client.send(packet);
                    timer.get(CurSeq + 1).cancel();
                    timer.put(CurSeq + 1, new UDT_Timer());
                    timer.get(CurSeq + 1).schedule(new TahoeRetran(client, packet), 3000, 3000);
                }
                System.out.println("cwnd:" + cwnd + "->" + (Math.max(cwnd / 2, 2) + 3));
                System.out.println("ssthresh:" + ssthresh + "->" + Math.max(cwnd / 2, 2));
                System.out.println();
                ssthresh = Math.max(cwnd / 2, 2);
                cwnd = ssthresh + 3;
            }
        }
    }
    class TahoeRetran extends UDT_RetransTask {
        int CurSeq;
        private TCP_PACKET packet;
        public TahoeRetran(Client client, TCP_PACKET packet) {
            super(client, packet);
            CurSeq = packet.getTcpH().getTh_seq();
            this.packet = packet;
        }
        @Override
        public void run() {
            System.out.println("***********��ʱ�ش�***********");
            System.out.println("ssthresh:" + ssthresh + "->" + Math.max(cwnd / 2, 2));
            System.out.println("\ncwnd��Ϊ1\n");
            ssthresh = Math.max(cwnd / 2, 2);
            cwnd = 1;
            super.run();
        }
    }
}
