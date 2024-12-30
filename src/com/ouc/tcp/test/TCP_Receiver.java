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

    private TCP_PACKET ackPack;	//�ظ���ACK���Ķ�
    //int sequence=1;//���ڼ�¼��ǰ�����յİ���ţ�ע�����Ų���ȫ��
    //int last_seq=0;//�ϸ�����sequence

    //׼�����մ���
    private Receiver_Window ReceiverWindow=new Receiver_Window(this.client);

    //���tahoe����
    int Next_seq=0;//ϣ���յ���seq
    private Hashtable<Integer,TCP_PACKET>packets2=new Hashtable<>();

    /*���캯��*/
    public TCP_Receiver() {
        super();	//���ó��๹�캯��
        super.initTCP_Receiver(this);	//��ʼ��TCP���ն�
        this.ReceiverWindow.init();
    }

    @Override
    //���յ����ݱ������У��ͣ����ûظ���ACK���Ķ�
    public void rdt_recv(TCP_PACKET recvPack) {

        //���У���룬����ACK
        if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
/*
            int current_seq=recvPack.getTcpH().getTh_seq();
            //����ACK���ĶΣ�����ȷ�Ϻţ�
            tcpH.setTh_ack(current_seq);

            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));//���ûظ�ack��checksum
            //��û�г������ǲ����ظ���,���շ���ack�ͷ��ͷ��İ�seq�й�
            if(!(current_seq==last_seq)) {
                //�����ڣ�˵�����ظ�������last_seqΪ��ǰ��seq
                last_seq = current_seq;

                //���Ǵ����Ҳ�����ظ������������մ��ڴ���
                ReceiverWindow.recvPacket(recvPack);
                //�����յ�����ȷ��������ݲ���data���У�׼������
                dataQueue.add(recvPack.getTcpS().getData());
                sequence++;
            }
            //�ظ�ACK���Ķ�
            reply(ackPack);
        }else{
            System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
            System.out.println("Recieved Packet"+recvPack.getTcpH().getTh_sum());
            System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
            tcpH.setTh_ack(last_seq);
            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            //�ظ�ACK���Ķ�
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
                // ��������CurSeq֮�������������ݼ������ݶ���
                while(packets2.containsKey(Next_seq)){
                    dataQueue.add(packets2.get(Next_seq).getTcpS().getData());
                    packets2.remove(Next_seq); //�ӽ��շ��������Ƴ�
                    Next_seq++;
                }
                //ÿ20�����ݽ���һ��
                if(dataQueue.size() >= 20)
                    deliver_data();
            } else {  // ����
                if (!packets2.containsKey(CurSeq) && CurSeq > Next_seq)
                    packets2.put(CurSeq, recvPack); // ������շ�����
            }
        }
        tcpH.setTh_ack((Next_seq - 1) * 100 + 1);//����ACK���Ķ�
        ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
        tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
        reply(ackPack);

//        System.out.println();
//
//        //�������ݣ�ÿ20�����ݽ���һ�Σ�
//        if(dataQueue.size() == 20)
//            deliver_data();
    }

    @Override
    //�������ݣ�������д���ļ���������Ҫ�޸�
    public void deliver_data() {
        //���dataQueue��������д���ļ�
        File fw = new File("recvData.txt");
        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(fw, true));

            //ѭ�����data�������Ƿ����½�������
            while(!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();

                //������д���ļ�
                for(int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();		//����������
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    //�ظ�ACK���Ķ�
    public void reply(TCP_PACKET replyPack) {
        //���ô�����Ʊ�־
        tcpH.setTh_eflag((byte)7);	//eFlag = 4
        //�������ݱ�
        client.send(replyPack);
    }

}
