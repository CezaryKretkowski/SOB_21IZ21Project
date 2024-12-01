package org.example.algorithm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ElectionService {
    private int vote;
    private final Server parent;
    private final DatagramSocket socket;

    public ElectionService(Server parent, DatagramSocket socket){
        this.parent = parent;
        this.socket = socket;
        vote = 1;
    }
    public void onVote() throws IOException {

        if(!parent.isCandidate){
            vote = 1;
        }
        else {
            Debug.log("Don't receive any message, resending votes!!");
        }
        parent.isCandidate = true;
        Debug.log("Server is candidate");
        sendVote();

    }
    public void sendVote() throws IOException {
        if(!socket.getBroadcast())
            socket.setBroadcast(true);
        Message message = new Message(Type.voteRequest,"I'm candidate vote for me");
        byte[] buffer = message.toJson().getBytes();
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), socket.getLocalPort());

        socket.send(packet);
    }
    public void onResponseReceive(DatagramPacket packet,Message message) throws SocketException {
        if(parent.isCandidate) {
            vote++;
            Debug.log("I received a message from voter! Total votes: " + vote);
            var requireVotes = (float)parent.getHostNumber()/2;
            if(vote > requireVotes)
            {
                parent.isCandidate = false;
                parent.isLeader = true;
                parent.isForwarded = false;
            }

        }
    }

    public void onRequestReceive(DatagramPacket packet,Message message) throws IOException {
        parent.isCandidate = false;
        parent.isForwarded = true;
        parent.isLeader = false;
        vote = 1;
        Message responseMessage = new Message(Type.voteResponse,"Response");
        byte[] buffer = responseMessage.toJson().getBytes();
        socket.setBroadcast(false);
        DatagramPacket responsePacket =
                new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());

        socket.send(responsePacket);
    }
}
