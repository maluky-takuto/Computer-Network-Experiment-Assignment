/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {

    private TCP_PACKET ackPack;	//回复的ACK报文段
    //int sequence=1;//用于记录当前待接收的包序号，注意包序号不完全是
    //int last_seq=0;//上个包的sequence

    //准备接收窗口
    private Receiver_Window ReceiverWindow=new Receiver_Window(this.client);

    //添加tahoe部分
    int Next_seq=0;//希望收到的seq
    private Hashtable<Integer,TCP_PACKET>packets2=new Hashtable<>();

    /*构造函数*/
    public TCP_Receiver() {
        super();	//调用超类构造函数
        super.initTCP_Receiver(this);	//初始化TCP接收端
        this.ReceiverWindow.init();
    }

    @Override
    //接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {

        //检查校验码，生成ACK
        if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
/*
            int current_seq=recvPack.getTcpH().getTh_seq();
            //生成ACK报文段（设置确认号）
            tcpH.setTh_ack(current_seq);

            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));//设置回复ack的checksum
            //包没有出错，看是不是重复包,接收方的ack和发送方的包seq有关
            if(!(current_seq==last_seq)) {
                //不等于，说明不重复，更新last_seq为当前的seq
                last_seq = current_seq;

                //不是错包，也不是重复包，交给接收窗口处理
                ReceiverWindow.recvPacket(recvPack);
                //将接收到的正确有序的数据插入data队列，准备交付
                dataQueue.add(recvPack.getTcpS().getData());
                sequence++;
            }
            //回复ACK报文段
            reply(ackPack);
        }else{
            System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
            System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
            System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
            tcpH.setTh_ack(last_seq);
            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            //回复ACK报文段
            reply(ackPack);*/
//            int ack;
//            ack=this.ReceiverWindow.recvPacket(recvPack);
//            if(ack>0){
//                dataQueue.add(recvPack.getTcpS().getData());
//                this.tcpH.setTh_ack(ack);
//                this.ackPack=new TCP_PACKET(this.tcpH,this.tcpS,recvPack.getSourceAddr());
//                this.tcpH.setTh_sum(CheckSum.computeChkSum(this.ackPack));
//                reply(this.ackPack);
//            }
            int CurSeq = (recvPack.getTcpH().getTh_seq() - 1) / 100;
            if (Next_seq == CurSeq) {
                dataQueue.add(recvPack.getTcpS().getData());
                Next_seq++;
                // 将窗口中CurSeq之后连续包的数据加入数据队列
                while(packets2.containsKey(Next_seq)){
                    dataQueue.add(packets2.get(Next_seq).getTcpS().getData());
                    packets2.remove(Next_seq); //从接收方窗口中移出
                    Next_seq++;
                }
                //每20组数据交付一次
                if(dataQueue.size() >= 20)
                    deliver_data();
            } else {  // 无序
                if (!packets2.containsKey(CurSeq) && CurSeq > Next_seq)
                    packets2.put(CurSeq, recvPack); // 加入接收方窗口
            }
        }
        tcpH.setTh_ack((Next_seq - 1) * 100 + 1);//生成ACK报文段
        ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
        tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
        reply(ackPack);

//        System.out.println();
//
//        //交付数据（每20组数据交付一次）
//        if(dataQueue.size() == 20)
//            deliver_data();
    }

    @Override
    //交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        //检查dataQueue，将数据写入文件
        File fw = new File("recvData.txt");
        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(fw, true));

            //循环检查data队列中是否有新交付数据
            while(!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();

                //将数据写入文件
                for(int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();		//清空输出缓存
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    //回复ACK报文段
    public void reply(TCP_PACKET replyPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte)7);	//eFlag = 4
        //发送数据报
        client.send(replyPack);
    }

}
