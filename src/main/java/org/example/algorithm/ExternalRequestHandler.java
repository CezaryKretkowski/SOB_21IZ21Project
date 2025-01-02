package org.example.algorithm;

import java.io.IOException;
import java.net.*;

public class ExternalRequestHandler extends Thread {
    private final Server server;
    private final DatagramSocket externalSocket;
    private volatile boolean running;

    public ExternalRequestHandler(Server server, int port) throws SocketException {
        this.server = server;
        this.externalSocket = new DatagramSocket(port);
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

                if (server.isLeader) {
                    String response = "Request handled by leader: " + server.getHostNumber();
                    DatagramPacket responsePacket = new DatagramPacket(
                            response.getBytes(),
                            response.getBytes().length,
                            packet.getAddress(),
                            packet.getPort()
                    );
                    externalSocket.send(responsePacket);
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
                    } else {
                        System.out.println("No leader available to forward the request.");
                    }
                }
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

