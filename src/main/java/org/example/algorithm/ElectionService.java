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
    private final LogHandler logHandler; // Interfejs do obsługi logów

    public ElectionService(Server parent, DatagramSocket socket, LogHandler logHandler) {
        this.parent = parent;
        this.socket = socket;
        this.logHandler = logHandler; // Ustawienie handlera logów
        vote = 1;
    }

    public void onVote() throws IOException {
        if (!parent.isCandidate) {
            vote = 1;
        } else {
            logHandler.log("Don't receive any message, resending votes!!");
        }
        parent.isCandidate = true;
        logHandler.log("Server is now a candidate.");
        sendVote();
    }

    public void sendVote() throws IOException {
        if (!socket.getBroadcast()) {
            socket.setBroadcast(true);
        }
        Message message = new Message(Type.voteRequest, "I'm candidate; vote for me.");
        byte[] buffer = message.toJson().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), socket.getLocalPort());

        socket.send(packet);
        logHandler.log("Vote request broadcasted.");
    }

    public void onResponseReceive(DatagramPacket packet, Message message) throws SocketException {
        if (parent.isCandidate) {
            vote++;
            logHandler.log("Received a vote! Total votes: " + vote);
            var requiredVotes = (float) parent.getHostNumber() / 2;
            if (vote > requiredVotes) {
                parent.isCandidate = false;
                parent.isLeader = true;
                parent.isForwarded = false;
                logHandler.log("Server is now the leader.");
            }
        }
    }

    public void onRequestReceive(DatagramPacket packet, Message message) throws IOException {
        parent.isCandidate = false;
        parent.isForwarded = true;
        parent.isLeader = false;
        vote = 1;
        logHandler.log("Vote request received. Forwarding vote response.");

        Message responseMessage = new Message(Type.voteResponse, "Response");
        byte[] buffer = responseMessage.toJson().getBytes();
        socket.setBroadcast(false);
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());

        socket.send(responsePacket);
        logHandler.log("Vote response sent to: " + packet.getAddress().getHostAddress());
    }
}
