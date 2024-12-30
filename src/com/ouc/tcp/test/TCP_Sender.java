/***************************2.1: ACK/NACK
 **************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Sender extends TCP_Sender_ADT {

    private TCP_PACKET tcpPack;	//待发送的TCP数据报
    private volatile int flag = 4;
    //添加2个变量，准备使用计时器
//    private UDT_RetransTask retrans_task;
//    private UDT_Timer timer;

    //准备滑动窗口
    private Sender_Window SenderWindow = new Sender_Window(this.client);

    /*构造函数*/
    public TCP_Sender() {
        super();	//调用超类构造函数
        super.initTCP_Sender(this);		//初始化TCP发送端
        this.SenderWindow.init();
    }

    @Override
    //可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
    public void rdt_send(int dataIndex, int[] appData) {



        //生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
        tcpH.setTh_seq(dataIndex * appData.length + 1);//包序号设置为字节流号：
        tcpS.setData(appData);
        tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
        //更新带有checksum的TCP 报文头
        tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
        tcpPack.setTcpH(tcpH);

        //发送TCP数据报，3.0，添加计时器
//        retrans_task=new UDT_RetransTask(client,tcpPack);
//        timer=new UDT_Timer();
//        timer.schedule(retrans_task,2000,3000);
//        udt_send(tcpPack);
//        flag = 0;

        //发送TCP数据报,SR版，用senderwindow托管
        //如果窗口不能动

        if(this.SenderWindow.isFull()){//满了
            System.out.println("**sender**window**is**wait**");
            flag = 0;
        }
        //等待ACK报文
        //waitACK();
        while (flag==0);

        //发包，先将包加入窗口
        try {
            this.SenderWindow.TakePacket(this.tcpPack.clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        udt_send(tcpPack);
    }

    @Override
    //不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
    public void udt_send(TCP_PACKET stcpPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte)7);  //eFlag =4,错误和丢失
        //System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());
        //发送数据报
        client.send(stcpPack);
    }


    //需要修改
    public void waitACK(TCP_PACKET packet) {
        int currentAck=packet.getTcpH().getTh_ack();
        /*int currentAck = ackQueue.poll();//从ack队列中拿出来*/
        // System.out.println("CurrentAck: "+currentAck);

        if (CheckSum.computeChkSum(packet)==packet.getTcpH().getTh_sum()) {//说明收到ack包了
            this.SenderWindow.recvAck(currentAck);//交给窗口处理
            if(!this.SenderWindow.isFull()) {
                flag=1;//窗口不满，就可以发新包
            }else{
                flag=0;
            }
//                System.out.println("Clear: " + tcpPack.getTcpH().getTh_seq());
            //timer.cancel();
            //break;
        } /*else {//重发并再次等待ack报文
            System.out.println("Retransmit: " + tcpPack.getTcpH().getTh_seq());
            udt_send(tcpPack);

            //flag = 0;
        }*/

    }

    @Override
    //接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
    public void recv(TCP_PACKET recvPack) {
        System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
        ackQueue.add(recvPack.getTcpH().getTh_ack());
        System.out.println();
        //处理ACK报文
        waitACK(recvPack);
    }

    @Override
    public void waitACK() {

    }

}
