package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.client.UDT_Timer;

import javax.swing.undo.CannotUndoException;
import java.util.Hashtable;

public class Sender_Window {
    private Client client;
    private int SenderWinSize = 64; //���ͷ����ڴ�С

    private mypacket[] packets=new mypacket[2*SenderWinSize];

    private int offset=0;

    //����Tahoe,reno����ı���
    int ssthresh=16;//����
    int cwnd=1;//ӵ������
    int front_ack_seq=-1;//��һ��ack����seq,���ش�Ҫ��
    int repeat_ack=0;//�ظ����ٴ�
    int CA_num=0;//ӵ������ʱ�յ���ack��
    private Hashtable<Integer,TCP_PACKET> packets2=new Hashtable<>();
    private Hashtable<Integer,UDT_Timer> timer2=new Hashtable<>();

    public Sender_Window(Client client) {
        this.client = client;
    }

    public void init(){
        for(int i=0;i<=2*SenderWinSize-1;i++){
            this.packets[i]=new mypacket();
        }
    }

    public boolean isFull(){//�ǲ��Ƿ������ݰ�
//        for(int i=0;i<=SenderWinSize-1;i++){
//            if(this.packets[i].getPacket()==null){
//                return false;
//            }
//        }
//        return true;
        return this.cwnd<=packets2.size();
    }

    public boolean isWait() {
        return this.packets[0].getPacket()!=null && !this.packets[0].isAcked();
    }


    public void TakePacket(TCP_PACKET packet) {
        //�жϰ��ڲ��ڴ���֮�ڣ������,���ʱ��
//        int temp_seq=(packet.getTcpH().getTh_seq()-1)/100;
//        temp_seq-=offset;
//        if(0<=temp_seq && temp_seq<=SenderWinSize-1){
//            UDT_Timer timer=new UDT_Timer();
//            this.packets[temp_seq]=new mypacket(packet,timer);
//            timer.schedule(new UDT_RetransTask(client,packet),3000,3000 );
        int Current_seq=(packet.getTcpH().getTh_seq()-1)/100;
        timer2.put(Current_seq,new UDT_Timer());
        timer2.get(Current_seq).schedule(new TahoeRetran(client,packet),3000,3000);
        packets2.put(Current_seq,packet);
        }

    public void recvAck(int Current_Ack) {//��ack�ù���
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
        if (Current_Ack != this.front_ack_seq) { //�µ�����ACK��
            for (int i = this.front_ack_seq + 1; i <= Current_Ack; i++) {
                packets2.remove(i);
                if (timer2.containsKey(i)) {
                    timer2.get(i).cancel();
                    timer2.remove(i);
                }
            }
            this.front_ack_seq = Current_Ack;
            this.repeat_ack = 0;
            if (cwnd < ssthresh) { //����ʼ�㷨
                System.out.println("----------ִ������ʼ�㷨----------");
                System.out.println("��������ʼ�㷨��ÿ�յ�һ��ACK�ͽ�cwnd����1�����cwnd��" + cwnd + "����Ϊ" + (cwnd + 1));
                System.out.println();
                cwnd++;
            } else { //ӵ������
                this.CA_num++;
                System.out.println("----------ִ��ӵ�������㷨----------");
                System.out.println("����ӵ�������㷨��ÿ����һ��RTT�Ű�cwnd����1����ʱcwndΪ" + cwnd + "��ӵ������RTT�Ľ���Ϊ" + this.CA_num);
                System.out.println("ֻ�е�RTT���ȴﵽ" + cwnd + "ʱ�Ű�cwnd����1\n");
                if (this.CA_num >= cwnd) {  // �յ�һ��RTT��ACK�������� cwnd
                    this.CA_num -= cwnd;  // ����RTT����
                    System.out.println("��ʱRTT���ȴﵽcwnd��cwnd��Ҫ����1�����cwnd��" + cwnd + "����Ϊ" + (cwnd + 1));
                    System.out.println();
                    cwnd++;
                }
            }
        } else { //������ظ���ACK��
            this.repeat_ack++;
            if(this.repeat_ack >= 3){ //�ظ��յ�3��
                TCP_PACKET packet = packets2.get(Current_Ack + 1);
                if (packet != null) {
                    System.out.println("----------ִ���ش�----------");
                    client.send(packet);
                    timer2.get(Current_Ack + 1).cancel();
                    timer2.put(Current_Ack + 1, new UDT_Timer());
                    timer2.get(Current_Ack + 1).schedule(new TahoeRetran(client, packet), 3000, 3000);
                }
                System.out.println("���ݿ��ش��㷨��Ӧ�ý�cwnd��" + cwnd + "��Ϊ" + 1);
                System.out.println("ssthreshӦ�ñ�Ϊӵ�����ڵ�һ�룬���ssthresh��" + ssthresh + "��Ϊ" + Math.max(cwnd / 2, 2));
                System.out.println();
                ssthresh = Math.max(cwnd / 2, 2);
                cwnd = 1;
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
            System.out.println("����������������������ʱ�ش���������������������");
            System.out.println("��ʱ�ش�Ӧ����ssthresh�趨Ϊӵ�����ڵ�һ�룬���ssthresh��" + ssthresh + "��Ϊ" + Math.max(cwnd / 2, 2));
            System.out.println("\n��cwnd��Ϊ1\n");
            ssthresh = Math.max(cwnd / 2, 2);
            cwnd = 1;
            super.run();
        }
    }
}
