package org.example.algorithm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class HeartBeatService {
    private final Server parent;
    private final DatagramSocket socket;
    public HeartBeatService(Server parent,DatagramSocket socket){
        this.parent = parent;
        this.socket = socket;
    }
    public void onReceive(DatagramPacket packet, Message message) throws IOException {

        if(parent.isForwarded) {
            whenForwarder(packet);
        }
        if(parent.isLeader){
            whenLeader(packet);
        }
    }
    private void whenForwarder(DatagramPacket packet) throws IOException {
        Message message = new Message(Type.heartBeat,"HartBeat return");
        System.out.println("Heart beat message token when Forwarder: " + message.token);
        String jsonMessage = message.toJson();
        byte[] buf = jsonMessage.getBytes();
        DatagramPacket returnPackage = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
        socket.send(returnPackage);

    }
    private void whenLeader(DatagramPacket packet){
        boolean isAny = parent.forwardersAddresses.stream().anyMatch(x->x.equals(packet.getAddress()));
        if(!isAny){
            parent.forwardersAddresses.add(packet.getAddress());
        }
    }
    public void sendHeartBeat() throws IOException {
        if(!socket.getBroadcast())
            socket.setBroadcast(true);

        Message message = new Message(Type.heartBeat,"heart beat");
        System.out.println("Send heart beat: " + message.token);
        byte[] buffer = message.toJson().getBytes();
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), socket.getLocalPort());

        socket.send(packet);
    }

}
