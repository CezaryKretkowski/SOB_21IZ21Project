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
    public void onRequestReceive(DatagramPacket packet, Message message) throws IOException {

        if(parent.isForwarded) {
            try {
                Debug.log("Server is forwarder");
                HeartBeatMessageContent content = new HeartBeatMessageContent(message.content);
                if (content.hostNumber != parent.getHostNumber()) {
                    parent.setHostNumber(content.hostNumber);
                    parent.forwardersAddresses.clear();
                }
            } catch (Exception e) {
                Debug.log("Failed to get content "+message.content);
            }
            whenForwarder(packet);
            if(parent.getLeaderAddress()!= null && !parent.getLeaderAddress().equals(packet.getAddress())){
                parent.setLeaderAddress(packet.getAddress());
            }
        }

        if(parent.isCandidate || parent.isLeader)
        {
            Debug.log("Server is forwarder");
            parent.electionService.RestVote();
            parent.isCandidate = false;
            parent.isForwarded =true;
            parent.isLeader = false;
            whenForwarder(packet);
        }
    }
    private void whenForwarder(DatagramPacket packet) throws IOException {
        Message message = new Message(Type.heartBeatResponse,"HartBeat return");
        String jsonMessage = message.toJson();
        byte[] buf = jsonMessage.getBytes();
        DatagramPacket returnPackage = new DatagramPacket(buf, buf.length, packet.getAddress(),
                packet.getPort());
        socket.send(returnPackage);
    }

    public void sendHeartBeat() throws IOException {
        if(!socket.getBroadcast())
            socket.setBroadcast(true);
        String jsonMessage = new HeartBeatMessageContent(parent.getHostNumber(), "heartbeat").toJson();
        Message message = new Message(Type.heartBeatRequest,jsonMessage);
        byte[] buffer = message.toJson().getBytes();
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"),
                        socket.getLocalPort());

        socket.send(packet);
    }

    public void onRequestResponse(DatagramPacket packet, Message message) {
    }
}
