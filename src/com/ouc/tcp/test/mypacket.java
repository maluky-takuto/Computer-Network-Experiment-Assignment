package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Timer;

public class mypacket {
    private TCP_PACKET packet;
    private UDT_Timer timer;
    private boolean acked;

    public TCP_PACKET getPacket() {
        return packet;
    }

    public void setPacket(TCP_PACKET packet) {
        this.packet = packet;
    }

    public UDT_Timer getTimer() {
        return timer;
    }

    public void setTimer(UDT_Timer timer) {
        this.timer = timer;
    }

    public boolean isAcked() {
        return acked;
    }

    public void setAcked(boolean acked) {
        this.acked = acked;
    }

    public mypacket(TCP_PACKET packet) {
        this.packet = packet;
        this.timer=null;
        this.acked=false;
    }

    public mypacket(TCP_PACKET packet, UDT_Timer timer) {
        this.packet = packet;
        this.timer = timer;
        this.acked=false;
    }
    public mypacket() {
        this.packet = null;
        this.timer = null;
        this.acked=false;
    }

}
