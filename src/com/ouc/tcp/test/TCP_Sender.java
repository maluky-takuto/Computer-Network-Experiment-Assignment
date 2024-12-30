/***************************2.1: ACK/NACK
 **************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Sender extends TCP_Sender_ADT {

    private TCP_PACKET tcpPack;	//�����͵�TCP���ݱ�
    private volatile int flag = 4;
    //���2��������׼��ʹ�ü�ʱ��
//    private UDT_RetransTask retrans_task;
//    private UDT_Timer timer;

    //׼����������
    private Sender_Window SenderWindow = new Sender_Window(this.client);

    /*���캯��*/
    public TCP_Sender() {
        super();	//���ó��๹�캯��
        super.initTCP_Sender(this);		//��ʼ��TCP���Ͷ�
        this.SenderWindow.init();
    }

    @Override
    //�ɿ����ͣ�Ӧ�ò���ã�����װӦ�ò����ݣ�����TCP���ݱ�����Ҫ�޸�
    public void rdt_send(int dataIndex, int[] appData) {



        //����TCP���ݱ���������ź������ֶ�/У���),ע������˳��
        tcpH.setTh_seq(dataIndex * appData.length + 1);//���������Ϊ�ֽ����ţ�
        tcpS.setData(appData);
        tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
        //���´���checksum��TCP ����ͷ
        tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
        tcpPack.setTcpH(tcpH);

        //����TCP���ݱ���3.0����Ӽ�ʱ��
//        retrans_task=new UDT_RetransTask(client,tcpPack);
//        timer=new UDT_Timer();
//        timer.schedule(retrans_task,2000,3000);
//        udt_send(tcpPack);
//        flag = 0;

        //����TCP���ݱ�,SR�棬��senderwindow�й�
        //������ڲ��ܶ�

        if(this.SenderWindow.isFull()){//����
            System.out.println("**sender**window**is**wait**");
            flag = 0;
        }
        //�ȴ�ACK����
        //waitACK();
        while (flag==0);

        //�������Ƚ������봰��
        try {
            this.SenderWindow.TakePacket(this.tcpPack.clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        udt_send(tcpPack);
    }

    @Override
    //���ɿ����ͣ�������õ�TCP���ݱ�ͨ�����ɿ������ŵ����ͣ������޸Ĵ����־
    public void udt_send(TCP_PACKET stcpPack) {
        //���ô�����Ʊ�־
        tcpH.setTh_eflag((byte)7);  //eFlag =4,����Ͷ�ʧ
        //System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());
        //�������ݱ�
        client.send(stcpPack);
    }


    //��Ҫ�޸�
    public void waitACK(TCP_PACKET packet) {
        int currentAck=packet.getTcpH().getTh_ack();
        /*int currentAck = ackQueue.poll();//��ack�������ó���*/
        // System.out.println("CurrentAck: "+currentAck);

        if (CheckSum.computeChkSum(packet)==packet.getTcpH().getTh_sum()) {//˵���յ�ack����
            this.SenderWindow.recvAck(currentAck);//�������ڴ���
            if(!this.SenderWindow.isFull()) {
                flag=1;//���ڲ������Ϳ��Է��°�
            }else{
                flag=0;
            }
//                System.out.println("Clear: " + tcpPack.getTcpH().getTh_seq());
            //timer.cancel();
            //break;
        } /*else {//�ط����ٴεȴ�ack����
            System.out.println("Retransmit: " + tcpPack.getTcpH().getTh_seq());
            udt_send(tcpPack);

            //flag = 0;
        }*/

    }

    @Override
    //���յ�ACK���ģ����У��ͣ���ȷ�ϺŲ���ack����;NACK��ȷ�Ϻ�Ϊ��1������Ҫ�޸�
    public void recv(TCP_PACKET recvPack) {
        System.out.println("Receive ACK Number�� "+ recvPack.getTcpH().getTh_ack());
        ackQueue.add(recvPack.getTcpH().getTh_ack());
        System.out.println();
        //����ACK����
        waitACK(recvPack);
    }

    @Override
    public void waitACK() {

    }

}
