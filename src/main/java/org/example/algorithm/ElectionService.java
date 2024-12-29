package org.example.algorithm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.UUID;

public class ElectionService {
    private int vote;
    private final Server parent;
    private final DatagramSocket socket;
    private int voteNumber = 0;
    private String voteKey;

    public void RestVote(){
        vote =1;
        voteNumber =0;
    }
    public ElectionService(Server parent, DatagramSocket socket){
        this.parent = parent;
        this.socket = socket;
        vote = 1;
    }
    public void onFailedElection(){

        if(voteNumber<3)
            return;

        voteNumber = 0;

        if(parent.forwardersAddresses.size()>1) {
            parent.setHostNumber(parent.forwardersAddresses.size() + 1);
            parent.forwardersAddresses.clear();
        }
    }

    public void onVote() throws IOException {
        Random rnd = new Random();
        voteNumber++;
        vote = 1;
        voteKey = UUID.randomUUID().toString();
        if(voteNumber>0){
            Debug.log("Don't receive any message, resending votes!!: Vote number :"+voteNumber);
        }
        parent.setRndTimeOut(parent.getTimeOut()-rnd.nextInt(10,80 ));
        parent.isCandidate = true;
        Debug.log("Server is candidate");
        sendVote();
        onFailedElection();

    }
    public void sendVote() throws IOException {
        if(!socket.getBroadcast())
            socket.setBroadcast(true);
        Message message = new Message(Type.voteRequest,voteKey);
        byte[] buffer = message.toJson().getBytes();
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"),
                        socket.getLocalPort());

        socket.send(packet);
    }
    public void onResponseReceive(DatagramPacket packet,Message message) throws SocketException {
        if(parent.isCandidate && message.content.equals(voteKey)) {
            vote++;
            Debug.log("I received a message from voter! Total votes: " + vote);
            var requireVotes = (float)parent.getHostNumber()/2;
            if(vote > requireVotes)
            {
                Debug.log("Server is leader!!");
                parent.isCandidate = false;
                parent.isLeader = true;
                parent.isForwarded = false;
                vote=1;
                voteNumber = 0;
            }

        }
    }

    public void onRequestReceive(DatagramPacket packet,Message message) throws IOException {
        parent.isCandidate = false;
        parent.isForwarded = true;
        parent.isLeader = false;
        vote = 1;
        Debug.log("Server is forwarder election service");
        Message responseMessage = new Message(Type.voteResponse, message.content);
        byte[] buffer = responseMessage.toJson().getBytes();
        socket.setBroadcast(false);
        DatagramPacket responsePacket =
                new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());

        socket.send(responsePacket);
    }
}
