package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.client.UDT_Timer;

import javax.swing.undo.CannotUndoException;
import java.util.Hashtable;

public class Sender_Window {
    private Client client;
    private int SenderWinSize = 64; //发送方窗口大小

    private mypacket[] packets=new mypacket[2*SenderWinSize];

    private int offset=0;

    //新增Tahoe,reno所需的变量
    int ssthresh=16;//门限
    int cwnd=1;//拥塞窗口
    int front_ack_seq=-1;//上一次ack包的seq,快重传要用
    int repeat_ack=0;//重复多少次
    int CA_num=0;//拥塞避免时收到的ack数
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

    public boolean isFull(){//是不是放满数据包
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
        //判断包在不在窗口之内，加入包,设计时器
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

    public void recvAck(int Current_Ack) {//把ack拿过来
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
        if (Current_Ack != this.front_ack_seq) { //新到来的ACK包
            for (int i = this.front_ack_seq + 1; i <= Current_Ack; i++) {
                packets2.remove(i);
                if (timer2.containsKey(i)) {
                    timer2.get(i).cancel();
                    timer2.remove(i);
                }
            }
            this.front_ack_seq = Current_Ack;
            this.repeat_ack = 0;
            if (cwnd < ssthresh) { //慢开始算法
                System.out.println("----------执行慢开始算法----------");
                System.out.println("根据慢开始算法，每收到一个ACK就将cwnd增加1，因此cwnd由" + cwnd + "增长为" + (cwnd + 1));
                System.out.println();
                cwnd++;
            } else { //拥塞避免
                this.CA_num++;
                System.out.println("----------执行拥塞避免算法----------");
                System.out.println("根据拥塞避免算法，每经过一个RTT才把cwnd增加1，此时cwnd为" + cwnd + "，拥塞避免RTT的进度为" + this.CA_num);
                System.out.println("只有当RTT进度达到" + cwnd + "时才把cwnd增加1\n");
                if (this.CA_num >= cwnd) {  // 收到一个RTT内ACK数量超过 cwnd
                    this.CA_num -= cwnd;  // 重置RTT进度
                    System.out.println("此时RTT进度达到cwnd，cwnd需要增加1，因此cwnd由" + cwnd + "增长为" + (cwnd + 1));
                    System.out.println();
                    cwnd++;
                }
            }
        } else { //如果是重复的ACK包
            this.repeat_ack++;
            if(this.repeat_ack >= 3){ //重复收到3次
                TCP_PACKET packet = packets2.get(Current_Ack + 1);
                if (packet != null) {
                    System.out.println("----------执行重传----------");
                    client.send(packet);
                    timer2.get(Current_Ack + 1).cancel();
                    timer2.put(Current_Ack + 1, new UDT_Timer());
                    timer2.get(Current_Ack + 1).schedule(new TahoeRetran(client, packet), 3000, 3000);
                }
                System.out.println("根据快重传算法，应该将cwnd由" + cwnd + "变为" + 1);
                System.out.println("ssthresh应该变为拥塞窗口的一半，因此ssthresh由" + ssthresh + "变为" + Math.max(cwnd / 2, 2));
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
            System.out.println("！！！！！！！！！！超时重传！！！！！！！！！！");
            System.out.println("超时重传应当将ssthresh设定为拥塞窗口的一半，因此ssthresh由" + ssthresh + "变为" + Math.max(cwnd / 2, 2));
            System.out.println("\n将cwnd变为1\n");
            ssthresh = Math.max(cwnd / 2, 2);
            cwnd = 1;
            super.run();
        }
    }
}
