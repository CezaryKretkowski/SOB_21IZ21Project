package org.example.algorithm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class MainThread extends Thread{

    private final Server parent;
    private boolean isRunning;
    private final DatagramSocket socket;


    public MainThread(Server parent,DatagramSocket socket){
        this.parent = parent;
        this.socket = socket;
        isRunning = true;

    }

    public void stopThread(){
        isRunning = false;
    }
    @Override
    public void run() {
        byte[] buf = new byte[parent.getMessageSize()];
        while (isRunning){
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try
            {
                socket.receive(packet);
                if(!packet.getAddress().equals(socket.getLocalAddress())){
                    parent.onReceive(packet);
                }
                Thread.sleep(50);
                parent.sendHeartBeat();
            }
            catch (SocketTimeoutException e)
            {
                parent.onTimeOut();
            }
            catch (IOException e)
            {
                Debug.log(e.getMessage());
            } catch (InterruptedException e) {
                
            }


        }
    }
}
