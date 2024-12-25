package com.ouc.tcp.test;
import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.client.UDT_Timer;

public class Sender_Window {
    private Client client;
    private int SenderWinSize = 64; //发送方窗口大小

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

    public boolean isFull(){//是不是放满数据包
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
        //判断包在不在窗口之内，加入包,设计时器
        int temp_seq=(packet.getTcpH().getTh_seq()-1)/100;
        temp_seq-=offset;
        if(0<=temp_seq && temp_seq<=SenderWinSize-1){
            UDT_Timer timer=new UDT_Timer();
            this.packets[temp_seq]=new mypacket(packet,timer);
            timer.schedule(new UDT_RetransTask(client,packet),3000,3000 );
        }
    }

    public void recvAck(int Current_Ack) {//把ack拿过来
        int temp_ack=(Current_Ack-1)/100;
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
        }
    }
}
