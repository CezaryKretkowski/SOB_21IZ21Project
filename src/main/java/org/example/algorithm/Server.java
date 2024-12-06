package org.example.algorithm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class Server {
    private final LogHandler logHandler; // Interfejs do obsługi logów
    public boolean isLeader = false;
    public boolean isCandidate = false;
    public boolean isForwarded = false;
    public List<InetAddress> forwardersAddresses;
    private ElectionService electionService;
    private HeartBeatService heartBeatService;
    private MainThread mainThread;
    private int messageSize = 2048;
    private int hostNumber = 3;

    public Server(LogHandler logHandler) {
        this.logHandler = logHandler;
        forwardersAddresses = new LinkedList<>();
    }

    public void start(InetAddress address) throws SocketException {
        DatagramSocket socket = new DatagramSocket(4445, address);
        socket.setSoTimeout(500); // Możliwość konfiguracji timeoutu
        logHandler.log("Server started on: " + address.getHostAddress());

        electionService = new ElectionService(this, socket, logHandler);
        heartBeatService = new HeartBeatService(this, socket);

        mainThread = new MainThread(this, socket);
        mainThread.start();
    }

    public void stop() {
        if (mainThread != null) {
            mainThread.stopThread();
            logHandler.log("Server stopped.");
        }
    }

    public void onTimeOut() {
        logHandler.log("Time Out occurred.");
        try {
            if (!isLeader) {
                electionService.onVote();
            } else {
                logHandler.log("Server is leader; only one server is available.");
            }
        } catch (Exception e) {
            logHandler.log("Error during timeout: " + e.getMessage());
        }
    }

    public void onReceive(DatagramPacket packet) throws IOException {
        String jsonMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        Message message = new Message(jsonMessage);

        if (!message.verifyToken()) {
            logHandler.log("Unauthorized message received from: " + packet.getAddress().getHostAddress());
            return;
        }

        logHandler.log(message.type + ": " + packet.getAddress().getHostAddress());

        switch (message.type) {
            case voteRequest -> electionService.onRequestReceive(packet, message);
            case voteResponse -> electionService.onResponseReceive(packet, message);
            case heartBeat -> heartBeatService.onReceive(packet, message);
            default -> throw new RuntimeException("Bad type of message");
        }
    }

    public void sendHeartBeat() throws IOException {
        if (isLeader) {
            heartBeatService.sendHeartBeat();
            logHandler.log("Heartbeat sent by the leader.");
        }
    }

    public int getMessageSize() {
        return messageSize;
    }

    public int getHostNumber() {
        return hostNumber;
    }
}
