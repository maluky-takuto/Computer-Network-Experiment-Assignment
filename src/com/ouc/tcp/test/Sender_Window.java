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
    private int SenderWinSize = 64; //发送方窗口大小

    //private mypacket[] packets=new mypacket[2*SenderWinSize];

    private int offset=0;

    //新增Tahoe,reno所需的变量
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>(); // 存储窗口内的数据包
    private Hashtable<Integer, UDT_Timer> timer = new Hashtable<>(); // 存储每个数据包的计时器
    private volatile int ssthresh = 16;  //门限值
    public int cwnd = 1;  //拥塞窗口
    private int front_ack_seq = -1; //上一次收到ACK包的seq
    private int repeat_num = 0; //重复的Ack数
    private int count_ack = 0; // 进入拥塞避免状态时收到的ACK数，记录一个RTT收到ACK的进度


    public Sender_Window(Client client) {
        this.client = client;
    }

//    public void init(){
//        for(int i=0;i<=2*SenderWinSize-1;i++){
//            this.packets[i]=new mypacket();
//        }
//    }

    public boolean isFull(){//是不是放满数据包
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
        //判断包在不在窗口之内，加入包,设计时器
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

    public void recvAck(int CurSeq) {//把ack拿过来
        /*int temp_ack=(Current_Ack-1)/100;
        temp_ack-=offset;
        //在窗口内
        if(0<=temp_ack && temp_ack<=SenderWinSize-1){
            if(this.packets[temp_ack].getTimer()==null&&this.packets[temp_ack]!=null){
                return;//如果是重复的包，它的计时器已经在上次被停掉了
            }
            //正确收到的包，
            this.packets[temp_ack].setAcked(true);//表示收到
            this.packets[temp_ack].getTimer().cancel();//取消计时器
            this.packets[temp_ack].setTimer(null);

            //每次加包之后，进行窗口移动
            int baseline=0; //表示需要移动多少
            for(int i=0;i<=SenderWinSize-1;i++){
                if(this.packets[i].isAcked()){
                    baseline++;
                }else{
                    break;//没连上，退出
                }
            }
            //移动
            for(int i=0;i<=SenderWinSize-1;i++){
                this.packets[i]=this.packets[i+baseline];
            }
            offset+=baseline;//增加偏移量
        }*/
        if (CurSeq != front_ack_seq) { //新到来的ACK包
            for (int i = front_ack_seq + 1; i <= CurSeq; i++) {
                packets.remove(i);
                if (timer.containsKey(i)) {
                    timer.get(i).cancel();
                    timer.remove(i);
                }
            }
            front_ack_seq = CurSeq;
            repeat_num = 0;
            if (cwnd < ssthresh) { //慢开始算法
                System.out.println("***********慢开始***********");
                System.out.println(cwnd + "->" + (cwnd + 1));
                System.out.println();
                cwnd++;
            } else { //拥塞避免
                count_ack++;
                System.out.println("***********拥塞避免***********");
                System.out.println("cwnd:" + cwnd + "  RTT进度" + count_ack);
                // 如果一个RTT内ACK数量超过 cwnd
                if (count_ack >= cwnd) {
                    count_ack -= cwnd;//重置RTT进度
                    System.out.println("加法增大:" + cwnd + "->" + (cwnd + 1));
                    System.out.println();
                    cwnd++;
                }
            }
        } else { //如果是重复包
            repeat_num++;
            if(repeat_num >= 3){ //重复3次，要执行快恢复
                TCP_PACKET packet = packets.get(CurSeq + 1);
                if (packet != null) {
                    System.out.println("***********快恢复***********");
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
            System.out.println("***********超时重传***********");
            System.out.println("ssthresh:" + ssthresh + "->" + Math.max(cwnd / 2, 2));
            System.out.println("\ncwnd变为1\n");
            ssthresh = Math.max(cwnd / 2, 2);
            cwnd = 1;
            super.run();
        }
    }
}
