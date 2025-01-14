package org.example.algorithm;

import org.example.config.ConfigLoader;
import org.example.config.ServerConfig;
import org.example.data_base.DatabaseManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class Server {
    public boolean isLeader = false;
    public boolean isCandidate = false;
    public boolean isForwarded = true;
    public List<InetAddress> forwardersAddresses;
    public ElectionService electionService;
    private HeartBeatService heartBeatService;
    private MainThread mainThread;
    private int messageSize = 2048;

    private int hostNumber = 3;
    private int timeOut = 200;
    private int rndTimeOut = 200;

    private InetAddress leaderAddress;
    private DatagramSocket socket;
    private ExternalRequestHandler externalRequestHandler;

    private ServerConfig config;

    public Server() {
        forwardersAddresses = new LinkedList<>();
    }

    public void start(InetAddress address) throws SocketException {
        config = ConfigLoader.loadConfig("config.yaml"); // Wczytanie konfiguracji
        socket = new DatagramSocket(config.getServer().getPort(), address);
        hostNumber = config.getServer().getHostNumber();
        socket.setSoTimeout(config.getServer().getTimeout());
        electionService = new ElectionService(this, socket);
        heartBeatService = new HeartBeatService(this, socket);
        leaderAddress = address;
        mainThread = new MainThread(this, socket);
        mainThread.start();

        int apiPort = config.getServer().getApiPort();
        externalRequestHandler = new ExternalRequestHandler(this, apiPort, address);
        externalRequestHandler.start();
    }

    public void stop() {
        if (mainThread != null) {
            mainThread.stopThread();
        }
        if (externalRequestHandler != null) {
            externalRequestHandler.stopHandler();
        }
        if (socket != null) {
            socket.close();
        }
        isCandidate = false;
        isForwarded = true;
        isLeader = false;
    }

    public void onTimeOut() {
        Debug.log("Time Out");
        try {
            if (!isLeader) {
                electionService.onVote();
            } else {
                Debug.log("Server is leader, only one server is available");
            }
        } catch (Exception e) {
            Debug.log(e.getMessage());
        }
    }

    public void onReceive(DatagramPacket packet) throws IOException {
        String jsonMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        Message message = new Message(jsonMessage);

        if (!message.verifyToken()) {
            System.out.println("Message verification failed.");
            return;
        }

        switch (message.getType()) {
            case voteRequest -> electionService.onRequestReceive(packet, message);
            case voteResponse -> electionService.onResponseReceive(packet, message);
            case heartBeatRequest -> heartBeatService.onRequestReceive(packet, message);
            case heartBeatResponse -> heartBeatService.onRequestResponse(packet, message);
            default -> {
                System.out.println("Unrecognized message type: " + message.getType());
            }
        }
    }


    public void sendHeartBeat() throws IOException {
        if (isLeader) {
            heartBeatService.sendHeartBeat();
        }
    }

    public void upDateAddressList(DatagramPacket packet) {
        boolean isAny = forwardersAddresses.stream().anyMatch(x -> x.equals(packet.getAddress()));
        if (!isAny) {
            forwardersAddresses.add(packet.getAddress());
        }
    }

    public synchronized void setLeader(){
        this.isForwarded = false;
        this.isLeader = true;
        this.isCandidate = false;
        Debug.log("I am Leader now");
    }

    public ServerConfig getConfig() {
        return config;
    }

    public int getHostNumber() {
        return hostNumber;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public void setHostNumber(int hostNumber) {
        this.hostNumber = hostNumber;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public synchronized void SetLeader(){
        this.isForwarded = false;
        this.isLeader = true;
        this.isCandidate = false;
        Debug.log("Im now forwarder");
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public int getRndTimeOut() {
        return rndTimeOut;
    }

    public void setRndTimeOut(int rndTimeOut) {
        this.rndTimeOut = rndTimeOut;
    }

    public InetAddress getLeaderAddress() {
        return leaderAddress;
    }

    public List<InetAddress> getForwardersAddresses() {
        return forwardersAddresses;
    }

    public void setLeaderAddress(InetAddress leaderAddress) {
        this.leaderAddress = leaderAddress;
    }
}
