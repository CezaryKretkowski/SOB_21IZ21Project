package org.example.algorithm;

import org.example.data_base.DatabaseManager;

import java.io.IOException;
import java.net.*;

public class ExternalRequestHandler extends Thread {
    private final Server server;
    private final DatagramSocket externalSocket;
    private volatile boolean running;

    public ExternalRequestHandler(Server server, int port, InetAddress bindAddress) throws SocketException {
        this.server = server;
        this.externalSocket = new DatagramSocket(port, bindAddress);
        this.running = true;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        System.out.println("ExternalRequestHandler started on port: " + externalSocket.getLocalPort());
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                externalSocket.receive(packet);

                String receivedData = new String(packet.getData(), 0, packet.getLength());
                System.out.println("External request received: " + receivedData);

                String response;

                if (server.isLeader) {
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    response = dbManager.executeQuery(receivedData);
                } else {
                    InetAddress leaderAddress = server.getLeaderAddress();
                    if (leaderAddress != null) {
                        DatagramPacket forwardPacket = new DatagramPacket(
                                packet.getData(),
                                packet.getLength(),
                                leaderAddress,
                                server.getConfig().getServer().getApiPort()
                        );
                        externalSocket.send(forwardPacket);
                        System.out.println("Request forwarded to leader: " + leaderAddress.getHostAddress());
                        response = "Request forwarded to leader.";
                    } else {
                        response = "No leader available to handle the request.";
                    }
                }

                DatagramPacket responsePacket = new DatagramPacket(
                        response.getBytes(),
                        response.getBytes().length,
                        packet.getAddress(),
                        packet.getPort()
                );
                externalSocket.send(responsePacket);

            } catch (SocketException e) {
                if (running) {
                    System.out.println("SocketException in ExternalRequestHandler: " + e.getMessage());
                }
            } catch (IOException e) {
                System.out.println("Error handling external request: " + e.getMessage());
            }
        }
        externalSocket.close();
        System.out.println("ExternalRequestHandler stopped.");
    }

    public void stopHandler() {
        running = false;
        externalSocket.close();
    }
}


