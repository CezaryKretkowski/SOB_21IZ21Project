package org.example.algorithm;

import org.example.data_base.DatabaseManager;

import java.io.IOException;
import java.net.*;
import java.util.List;

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
        byte[] buffer = new byte[20048];
        System.out.println("ExternalRequestHandler started on port: " + externalSocket.getLocalPort());
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                externalSocket.receive(packet);

                if(packet.getAddress().equals(externalSocket.getLocalAddress()))
                    continue;

                String receivedData = new String(packet.getData(), 0, packet.getLength());
                System.out.println("External request received: " + receivedData);

                String response = "";
                ExternalClientMessage clientMessage = new ExternalClientMessage(receivedData);
                if (!clientMessage.isForwarder) {
                    clientMessage.port = packet.getPort();
                    clientMessage.address = packet.getAddress().getHostAddress();
                }
                if (clientMessage.isBroadcast) {

                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    response = "SQL Response From " + server.getHostNumber() + " " + dbManager.executeQuery(clientMessage.content);
                    System.out.println("SQL Response From Listener: " + response);
                }

                if (server.isLeader) {


                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    response = dbManager.executeQuery(clientMessage.content);
                    System.out.println("SQL Response From Leader: " + response);
                    //send out if not failed
                    if (!response.startsWith("SQL Error") && isReplicationNeeded(clientMessage.content)) {
                        System.out.println(clientMessage.content);

                        clientMessage.isForwarder = false;
                        clientMessage.isBroadcast = true;

                        String data = clientMessage.toJson();
                        System.out.println("data before Broadcasting: " + data);


                        try {

                            response = response;

                            DatagramPacket forwardPacket = new DatagramPacket(
                                    data.getBytes(),
                                    data.getBytes().length,
                                    InetAddress.getByName("255.255.255.255"),
                                    server.getConfig().getServer().getApiPort()
                            );
                            externalSocket.send(forwardPacket);

                        } catch (IOException e) {

                        }

                    }

                    DatagramPacket responsePacket = new DatagramPacket(
                            response.getBytes(),
                            response.getBytes().length,
                            InetAddress.getByName(clientMessage.address),
                            clientMessage.port //port klienta
                    );
                    externalSocket.send(responsePacket);
                }

                if (!server.isLeader && !clientMessage.isBroadcast) {
                    InetAddress leaderAddress = server.getLeaderAddress();
                    if (leaderAddress != null) {
                        System.out.println("Forwarding request to Leader...");
                        clientMessage.isForwarder = true;
                        clientMessage.isBroadcast = false;

                        String data = clientMessage.toJson();

                        DatagramPacket forwardPacket = new DatagramPacket(
                                data.getBytes(),
                                data.getBytes().length,
                                leaderAddress,
                                server.getConfig().getServer().getApiPort()
                        );
                        externalSocket.send(forwardPacket);
                        System.out.println("Request forwarded to leader: " + leaderAddress.getHostAddress());
                        response = response + " Request forwarded to leader.";
                    } else {
                        response = "No leader available to handle the request.";
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
    public boolean isReplicationNeeded(String content){
        return content.toLowerCase().startsWith("create") || content.toLowerCase().startsWith("update") || content.toLowerCase().startsWith("delete")
                || content.toLowerCase().startsWith("insert") ||  content.toLowerCase().startsWith("drop");
    }
    public void stopHandler() {
        running = false;
        externalSocket.close();
    }
}


