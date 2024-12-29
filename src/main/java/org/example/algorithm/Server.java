package org.example.algorithm;

import org.example.config.ConfigLoader;
import org.example.config.ServerConfig;

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

    public void setHostNumber(int hostNumber) {
        this.hostNumber = hostNumber;
    }

    private int hostNumber = 3;
    private DatagramSocket socket;
    private int timeOut = 200;

    public int getRndTimeOut() {
        return rndTimeOut;
    }

    public void setRndTimeOut(int rndTimeOut) {
        this.rndTimeOut = rndTimeOut;
    }

    private int rndTimeOut = 200;
    private InetAddress leaderAddress;

    public Server() {
        forwardersAddresses = new LinkedList<InetAddress>();

    }
    public void start(InetAddress address) throws SocketException {
        ServerConfig config = ConfigLoader.loadConfig("config.yaml");
        socket = new DatagramSocket(config.getServer().getPort(), address);
        hostNumber = config.getServer().getHostNumber();
        //to do możliwość konfiguracji timeout do przetestownia też
        socket.setSoTimeout(config.getServer().getTimeout());                                                                                       //to do możliwość konfiguracji timeout do przetestownia też
        electionService = new ElectionService(this,socket);
        heartBeatService = new HeartBeatService(this,socket);
        leaderAddress = null;
        mainThread = new MainThread(this, socket);
        mainThread.start();
    }
    public void stop(){

        mainThread.stopThread();
        socket.close();
        isCandidate = false;
        isForwarded =true;
        isLeader = false;
    }


    public void onTimeOut()  {
        Debug.log("Time Out");
        try {
            if (!isLeader)                                                                                                // jeśli jako lider dostaje time out oznacz że żadaen z serwerów nie działa poza liderem
                electionService.onVote();
            else
                Debug.log("Server is leader , only one server is available");

        }catch (Exception e){
            Debug.log(e.getMessage());
        }
    }

    public void onReceive(DatagramPacket packet) throws IOException {
        String jsonMessage = new String(packet.getData(),0, packet.getLength(), StandardCharsets.UTF_8);
        Message message = new Message(jsonMessage);

        if (!message.verifyToken()) {
            //Debug.log("Unauthorized message received from: " + packet.getAddress().getHostAddress());
            return;
        }

     //  Debug.log("host: "+getHostNumber());

        switch (message.type){
            case voteRequest -> electionService.onRequestReceive(packet,message);
            case voteResponse -> electionService.onResponseReceive(packet,message);
            case heartBeatRequest -> heartBeatService.onRequestReceive(packet,message);
            case heartBeatResponse -> heartBeatService.onRequestResponse(packet,message);
            default -> throw new RuntimeException("Bad type of message");
        }

    }

    public void sendHeartBeat() throws IOException {
        if(isLeader){
            heartBeatService.sendHeartBeat();
        }
    }

    public int getMessageSize() {
        return messageSize;
    }


    public int getHostNumber() {
        return hostNumber;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public InetAddress getLeaderAddress() {
        return leaderAddress;
    }

    public void setLeaderAddress(InetAddress leaderAddress) {
        this.leaderAddress = leaderAddress;
    }

    public void upDateAddressList(DatagramPacket packet){
        boolean isAny = forwardersAddresses.stream().anyMatch(x->x.equals(packet.getAddress()));
        if(!isAny){
            forwardersAddresses.add(packet.getAddress());
        }
    }
}
